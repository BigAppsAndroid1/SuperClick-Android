package il.co.superclick.toppings

import android.annotation.SuppressLint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.ui.getDrawable
import com.dm6801.framework.utilities.catch
import com.dm6801.framework.utilities.main
import il.co.superclick.MainActivity
import il.co.superclick.R
import il.co.superclick.data.*
import il.co.superclick.dialogs.ConfirmDialog
import il.co.superclick.dialogs.ProductAlertDialog
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.toppings.Pizza.rotate
import il.co.superclick.utilities.*
import kotlinx.android.synthetic.main.fragment_piza_topings.*
import kotlinx.android.synthetic.main.fragment_piza_topings.toppings_delete
import kotlinx.android.synthetic.main.fragment_toppings_selection.toppings_submit
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.set

class ToppingPizzaSelectionFragment : BaseFragment() {

    companion object {
        private val toppingsFragment: ToppingsFragment?
            get() = foregroundFragment as? ToppingsFragment

        fun create(
            index: Int,
            product: ShopProduct,
            cartItem: Cart.Item?,
            state: ToppingsState,
            level: Level? = null,
        ): ToppingPizzaSelectionFragment {
            return ToppingPizzaSelectionFragment()
                .apply {
                    this.product = product
                    this.cartItem = cartItem
                    this.state = state
                    this.index = index
                    this.level = level
                }
        }
    }

    private var level: Level? = null
    private lateinit var state: ToppingsState
    private lateinit var product: ShopProduct

    private var toppingsOnPizza: MutableMap<Int, MutableList<Int>> = mutableMapOf()
    private var listSelectedToppings = mutableListOf<ShopTopping>()
    private var cartItem: Cart.Item? = null
    private var index: Int = 1

    override val layout = R.layout.fragment_piza_topings
    override val themeBackground: Drawable? = getDrawable(R.drawable.dialog_background)

    private val recycler: RecyclerView? get() = pizza_toppings_list
    private val recyclerSelectedToppings: RecyclerView? get() = list_chosen_toppings

    private val toppingsImage: ImageView? get() = iv_base_pizza
    private val submitButton: TextView? get() = toppings_submit
    private val closeButton: ImageView? get() = dialog_toppings_close
    private val deleteButton: ImageView? get() = toppings_delete
    private val titleText: TextView? get() = pizza_title
    private val slices by lazy {
        mapOf(
            1 to slicePizza1,
            2 to slicePizza2,
            3 to slicePizza3,
            4 to slicePizza4
        )
    }
    private val slicePizza1: ImageView? get() = slice_pizza1
    private val slicePizza2: ImageView? get() = slice_pizza2
    private val slicePizza3: ImageView? get() = slice_pizza3
    private val slicePizza4: ImageView? get() = slice_pizza4
    private val isRealItem: Boolean get() = state.isSaved[index] == true
    private val isPizza: Boolean get() = product.product.productType == ProductType.PIZZA

    private val adapter: ToppingPizzaSelectionAdapter? get() = recycler?.adapter as? ToppingPizzaSelectionAdapter
    private val adapterSelectedToppings: SelectedPizzaToppingsAdapter? get() = recyclerSelectedToppings?.adapter as? SelectedPizzaToppingsAdapter
    private var toppingsAlertDelay: Deferred<Any>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.isVisible = false
        initTitle()
        if (product.product.productType == ProductType.PIZZA)
            toppingsImage?.glide(R.drawable.pizza)
        else
            toppingsImage?.glide(product.product.image)
        initClose()
        initDelete()
        initSubmit()
        if (!isRealItem) {
            if (product.shopProductOptions?.isNotEmpty() == true) {
                DialogChooseOptions.open(
                    product.product.optionsDescription,
                    product.shopProductOptions ?: return,
                    areOptionsForFree = level?.optionsPaid == 0
                ) { type ->
                    if (type == null) {
                        activity?.navigateBack()
                    } else {
                        initLists()
                        state.options.last().add(type)
                        listSelectedToppings.add(0, type)
                        setDataToAdapterSelectedToppings()
                        view.isVisible = true
                    }
                }
            } else {
                initLists()
                view.isVisible = true
            }
            initRecycler()
        } else {
            com.dm6801.framework.utilities.catch {
                view.isVisible = true
                product.toppings?.forEach { toppingsOnPizza[it.id] = mutableListOf() }
                initRecycler()
                listSelectedToppings.add(
                    0,
                    product.shopProductOptions?.firstOrNull {
                        it.id == (cartItem?.options?.get(index)?.first()?.first ?: -1)
                    } ?: return@catch
                )
                setDataToAdapterSelectedToppings()
            }
        }
    }

    private fun initLists() {
        state.selected.add(mutableListOf())
        state.options.add(mutableListOf())
        product.toppings?.forEach { topping ->
            toppingsOnPizza[topping.id] = mutableListOf()
        }
    }

    private fun drawToppings() {
        if (index < state.associatedToppings.size) {
            state.associatedToppings[index].forEachIndexed { i, pair ->
                pair.second.forEach {
                    drawTopping(it, pair.first)
                }
            }
        }
    }

    private fun initTitle() {
        titleText?.setThemeColor()
        titleText?.text = getString(
            R.string.toppings_header,
            product.product.name,
            if (product.product.toppingsDescription != null) product.product.toppingsDescription else "",
            index + 1
        )
    }

    private fun initRecycler() {
        recycler?.adapter = ToppingPizzaSelectionAdapter(
            level?.toppingsFree,
            if (level == null) null else level?.toppingsAddPaid == 1,
            product.product.productType,
            product.product.maxToppings,
            index
        ) { selected, listSelected ->
            if (product.product.productType == ProductType.PIZZA) {
                if (state.toppingsSlices.size <= index) state.toppingsSlices.add(mutableListOf())
                val isNewTopping =
                    state.toppingsSlices[index].firstOrNull { it.first == selected.first } == null
                val isEmpty = selected.second.isEmpty()
                if (isEmpty && isNewTopping) return@ToppingPizzaSelectionAdapter

                if (isNewTopping)
                    state.toppingsSlices[index].add(selected)
                else {
                    if (isEmpty)
                        state.toppingsSlices[index].removeAt(state.toppingsSlices[index].indexOfFirst { it.first == selected.first })
                    else
                        state.toppingsSlices[index][state.toppingsSlices[index].indexOfFirst { it.first == selected.first }] =
                            selected
                }
                state.selected[index] = listSelected

                product.toppings?.first { it.id == selected.first }?.let {
                    if (!listSelectedToppings.contains(it)) {
                        listSelectedToppings.add(it)
                    } else {
                        if (selected.second.isEmpty()) {
                            adapter?.selectedIds?.remove(element = it.id)
                            listSelectedToppings.remove(it)
                        }
                    }
                    setDataToAdapterSelectedToppings()
                }
                recyclerSelectedToppings?.adapter?.notifyDataSetChanged()
                recycler?.adapter?.notifyDataSetChanged()
                if (isNewTopping)
                    drawToppings()
                else
                    deleteToppingsOnPizza(selected.first)

                if (isRealItem)
                    invokeSaveCallback()
            } else {
                state.selected[index] = listSelected
                product.toppings?.first { it.id == selected.first }?.let {
                    if (!listSelectedToppings.contains(it)) {
                        listSelectedToppings.add(it)
                    } else {
                        if (selected.second.isEmpty()) {
                            listSelectedToppings.remove(it)
                            adapter?.selectedIds?.remove(element = selected.first)
                        }
                    }
                    setDataToAdapterSelectedToppings()
                }
                recyclerSelectedToppings?.adapter?.notifyDataSetChanged()
                recycler?.adapter?.notifyDataSetChanged()

                if (isRealItem) {
                    invokeSaveCallback()
                    toppingsAlertDelay?.cancel()
                    toppingsAlertDelay = delay(
                        1_000L,
                        Dispatchers.Main,
                        block = { ProductAlertDialog.productUpdated(product) })
                    main { toppingsAlertDelay?.start() }
                }
            }
        }

        recyclerSelectedToppings?.adapter =
            SelectedPizzaToppingsAdapter(
                toppingsFree = level?.toppingsFree,
                areOptionsFree = level?.optionsPaid == 0,
                product.shopProductOptions?.isNotEmpty() == true,
                state,
                index,
                onPizzaType = {
                    product.shopProductOptions?.let {
                        DialogChooseOptions.open(
                            product.product.optionsDescription,
                            it,
                            areOptionsForFree = level?.optionsPaid == 0
                        ) { type ->
                            type ?: return@open
                            state.options[index].removeFirst()
                            state.options[index].add(0, type)
                            listSelectedToppings.removeFirst()
                            listSelectedToppings.add(0, type)
                            setDataToAdapterSelectedToppings()
                            if (isRealItem)
                                invokeSaveCallback()
                        }
                    }

                }) { selected ->
                if (state.options[index].firstOrNull()?.topping?.name != selected.topping.name)
                    changeIsCheckedOnToppingPizzaSelectionAdapterItem(selected.id)
                state.selected[index].remove(selected)
                listSelectedToppings.remove(selected)
                adapter?.selectedIds?.remove(element = selected.id)
                if (product.product.productType == ProductType.PIZZA)
                    catch {
                        state.toppingsSlices.getOrNull(index)
                            ?.removeAt(state.toppingsSlices[index].indexOfFirst { it.first == selected.id })
                    }
                if (state.options[index].firstOrNull()?.topping?.name != selected.topping.name)
                    deleteToppingsOnPizza(selected.id)
                setDataToAdapterSelectedToppings()
                adapter?.notifyDataSetChanged()
                adapterSelectedToppings?.notifyDataSetChanged()
                if (isRealItem)
                    invokeSaveCallback()
            }

        submitToppingsList()
        drawToppings()
        if (state.selected.size <= index) listSelectedToppings.addAll(mutableListOf())
        else listSelectedToppings.addAll(state.selected[index])
        setDataToAdapterSelectedToppings()
    }

    private fun submitToppingsList() {
        if (state.associatedToppings.takeIf { it.isNotEmpty() }?.size ?: 0 > index) {
            cartItem?.run {
                val selected =
                    state.associatedToppings.takeIf { it.isNotEmpty() }?.get(index) ?: emptyList()
                adapter?.submitList(product?.toppings?.mapIndexed { index, topping ->
                    ToppingPizzaSelectionAdapter.Item(
                        index.toLong(),
                        topping,
                        topping.id in selected.map { it.first },
                        selected.firstOrNull { it.first == topping.id }?.second?.toMutableList()
                            ?: mutableListOf()
                    )
                })
                adapter?.selectedIds = selected.map { it.first }.toMutableList()
            } ?: adapter?.submitList(product.toppings ?: return)
        } else {
            adapter?.submitList(product.toppings ?: return)
        }
    }

    private fun initDelete() {
        if (state.isSaved[index] == true && level == null) {
            deleteButton?.isVisible = true
            deleteButton?.onClick(1_500) {
                ConfirmDialog.deleteAdditionalProduct {
                    toppingsFragment?.pizzaViewPagerAdapter?.run {
                        catch(silent = true) {
                            state.selected.takeIf { it.isNotEmpty() }?.removeAt(index)
                            state.isSaved.remove(index)
                            state.isSaved.keys.sortedBy { it }.run {
                                forEachIndexed { ind, i ->
                                    state.isSaved[ind] = (state.isSaved[i] == true)
                                }
                            }
                        }
                        state.count -= 1
                        if (state.selectedIds?.isNotEmpty() == true) {
                            notifyItemRemoved(index)
                            toppingsFragment?.initPager()
                            invokeSaveCallback()
                        } else {
                            toppingsFragment?.onToppingsSave?.invoke(null, null)
                            activity?.navigateBack()
                        }
                    }
                }
            }
        } else {
            deleteButton?.isVisible = false
        }
    }

    private fun setDataToAdapterSelectedToppings() {
        adapterSelectedToppings?.submitList(listSelectedToppings)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun changeIsCheckedOnToppingPizzaSelectionAdapterItem(idTopping: Int) {
        val topping = product.toppings?.firstOrNull { it.id == idTopping }
        val position = product.toppings?.indexOf(topping)
        val holder =
            position?.let { recycler?.layoutManager?.findViewByPosition(it) } as? ToppingPizzaSelectionAdapter.ViewHolder
        holder?.topping?.isChecked = false
        val adapter = recycler?.adapter as ToppingPizzaSelectionAdapter
        position?.let {
            adapter.currentList[it].isSelected = false
            adapter.currentList[it].slices = arrayListOf()
        }
        recycler?.adapter?.notifyDataSetChanged()
    }

    private fun deleteToppingsOnPizza(idTopping: Int) {
        if (isRealItem)
            invokeSaveCallback()
        for (i in 1..4) {
            deleteTopping(idTopping, i)
        }
    }

    private fun deleteTopping(idTopping: Int, slice: Int) {
        slices[slice]?.setImageBitmap(null)
        drawTopping(slice, idTopping)
    }

    private fun drawTopping(slice: Int, idTopping: Int) {
        val imageSlice = slices[slice]
        main {
            (imageSlice?.drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
                val bitmapRotated =
                    Pizza.getBitmapByCodename(product.toppings?.firstOrNull { it.id == idTopping }?.topping?.codename)?.rotate(
                        Pizza.getDegreeRotationImageTopping(slice)
                    )
                bitmapRotated?.let {
                    imageSlice.setImageBitmap(
                        Pizza.overlayBitmapToppings(
                            bitmap,
                            it
                        )
                    )
                }
            } ?: run {
                val toppingsList =
                    state.associatedToppings[index].filter { it.second.contains(slice) }
                val firstToppingCodeName =
                    product.toppings?.firstOrNull { topping -> toppingsList.firstOrNull { topping.id == it.first  } != null && topping.topping.codename != null }?.topping?.codename
                var bitmap = Pizza.getBitmapByCodename(firstToppingCodeName
                    ?: run { hideProgressBar(); return@main })?.rotate(
                    Pizza.getDegreeRotationImageTopping(
                        slice
                    )
                )
                if (toppingsList.size > 1)
                    for (i in 1..(toppingsList.size.minus(1))) {
                        val toppingCodeName =
                            product.toppings?.firstOrNull { it.id == toppingsList[i].first }?.topping?.codename
                        val bitmapRotated = Pizza.getBitmapByCodename(toppingCodeName)?.rotate(
                            Pizza.getDegreeRotationImageTopping(slice)
                        )
                        bitmap = bitmapRotated?.let {
                            bitmap?.let { it1 ->
                                Pizza.overlayBitmapToppings(
                                    it,
                                    it1
                                )
                            }
                        } ?: bitmap
                    }

                bitmap?.rotate(Pizza.getDegreeRotationImageTopping(slice))
                imageSlice?.setImageBitmap(bitmap)
            }
            hideProgressBar()
        }
    }

    @SuppressLint("UseCompatTextViewDrawableApis")
    private fun initSubmit() {
        submitButton?.run {
            backgroundTintList = mainColor

            text = if (!isRealItem || level != null)
                getString(R.string.add_to_cart)
            else
                if (isPizza) getString(R.string.add_new_pizza_with_toppings) else getString(R.string.add_new_product_with_toppings)

            onClick(1_500) {
                if (isRealItem && state.isSaved[state.isSaved.keys.lastOrNull()] == false
                    || state.isSaved.size < toppingsFragment?.pizzaViewPagerAdapter?.itemCount?.minus(
                        1
                    ) ?: 0
                ) {
                    toppingsFragment?.viewPager?.setCurrentItem(
                        toppingsFragment?.pizzaViewPagerAdapter?.itemCount?.minus(
                            1
                        ) ?: 0, true
                    )
                } else {
                    if (level != null) submitForFakeItem()
                    else submitForRealItem()
                }
            }
        }
    }

    private fun submitForRealItem() {
        lifecycleScope.launch {
            submitButton?.text =
                if (isPizza) getString(R.string.add_new_pizza_with_toppings)
                else getString(R.string.add_new_product_with_toppings)
            saveItemWithCallback()
            ConfirmDialog.additionalProduct(
                if (isPizza) R.string.toppings_pizza_additional_product_text else R.string.toppings_additional_product_text,
                onConfirm = { addNewFakeItem() },
                onCancel = { if (isRealItem) popFragemnt() else toppingsFragment?.onBackPressed() }
            )
        }
    }

    private fun submitForFakeItem() {
        lifecycleScope.launch {
            main { if (level == null) playCartSound() }
            if (level == null) {
                submitButton?.text =
                    if (isPizza) getString(R.string.add_new_pizza_with_toppings)
                    else getString(R.string.add_new_product_with_toppings)
                saveItemWithCallback()
                ProductAlertDialog.productAdded(product)
                delay(ProductAlertDialog.AUTO_CLOSE_MS + 100)
                ConfirmDialog.additionalProduct(
                    if (isPizza) R.string.toppings_pizza_additional_product_text else R.string.toppings_additional_product_text,
                    onConfirm = { addNewFakeItem() },
                    onCancel = { popFragemnt() }
                )
            } else {
                saveItemWithCallback()
                popFragemnt()
            }
        }
    }

    private fun addNewFakeItem() {
        state.count += 1
        toppingsFragment?.pizzaViewPagerAdapter?.run {
            toppingsFragment?.viewPager?.run {
                state.isSaved[adapter?.itemCount?.minus(1) ?: 0] = false
                setCurrentItem(adapter?.itemCount?.minus(1) ?: 0, true)
            }
            notifyItemInserted(index + 1)
        }
    }

    private fun saveItemWithCallback() {
        state.isSaved[index] = true
        invokeSaveCallback()
    }

    private fun invokeSaveCallback() {
        toppingsFragment?.onToppingsSave?.invoke(
            state.associatedToppings.filterIndexed { index, _ -> state.isSaved[index] == true },
            state.associatedOptions.filterIndexed { index, _ -> state.isSaved[index] == true }
        )
    }

    private fun initClose() {
        closeButton?.onClick(1_500) {
            activity?.navigateBack()
        }
    }

    private fun popFragemnt() {
        (foregroundActivity as MainActivity).popBackStack(
            FragmentManager.POP_BACK_STACK_INCLUSIVE,
            toppingsFragment?.tag
        )
    }

}