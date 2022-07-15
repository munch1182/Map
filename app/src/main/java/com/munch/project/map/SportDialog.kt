package com.munch.project.map

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.munch.lib.OnIndexListener
import com.munch.lib.extend.LinearLineItemDecoration
import com.munch.lib.extend.lazy
import com.munch.lib.extend.startActivity
import com.munch.lib.extend.toDateStr
import com.munch.lib.fast.base.BindBottomSheetDialogFragment
import com.munch.lib.recyclerview.BaseBindViewHolder
import com.munch.lib.recyclerview.BindRVAdapter
import com.munch.lib.recyclerview.setOnItemClickListener
import com.munch.project.map.databinding.ItemSportIdBinding
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.reflect.KClass

class SportDialog : BindBottomSheetDialogFragment() {

    private val rv by lazy { RecyclerView(ctx) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return rv
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(ctx, theme).apply {
            behavior.isDraggable = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = LinearLayoutManager(ctx)
        rv.layoutManager = lm
        rv.addItemDecoration(LinearLineItemDecoration(lm))
        val adapter =
            object : BindRVAdapter<SportIdRecord, ItemSportIdBinding>(ItemSportIdBinding::class) {
                override fun onBind(
                    holder: BaseBindViewHolder<ItemSportIdBinding>,
                    position: Int,
                    bean: SportIdRecord
                ) {
                    holder.bind.apply {
                        index.text = bean.id.toString()
                        time.text = "${bean.startTime.toDateStr()} 共${bean.count}条"
                    }
                }
            }
        rv.adapter = adapter
        ItemTouchHelper(SwipedItemCallback {
            lifecycleScope.launch {
                val record = adapter.get(it) ?: return@launch
                Record.del(record.id)
                adapter.remove(it)
                if (adapter.itemSize == 0) {
                    dialog?.cancel()
                }
            }
        }).attachToRecyclerView(rv)
        adapter.setOnItemClickListener { _, pos, _ ->
            MapActivity.show(ctx, adapter.get(pos)?.id ?: 0)
            dialog?.cancel()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            adapter.set(Record.querySportId())
        }
    }

    override val ctx: Context
        get() = requireContext()

    private class SwipedItemCallback(
        private val onIndex: OnIndexListener
    ) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT, ItemTouchHelper.LEFT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            onIndex.invoke(viewHolder.bindingAdapterPosition)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                val a = 1 - dX.absoluteValue / viewHolder.itemView.width
                viewHolder.itemView.alpha = a
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            viewHolder.itemView.alpha = 1f
            super.clearView(recyclerView, viewHolder)
        }

    }
}