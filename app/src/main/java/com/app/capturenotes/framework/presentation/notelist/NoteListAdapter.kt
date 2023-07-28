package com.app.capturenotes.framework.presentation.notelist

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.capturenotes.R
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.util.DateUtil
import com.app.capturenotes.framework.presentation.common.changeColor
import com.app.capturenotes.util.printLogD


class NoteListAdapter(
    private val interaction: Interaction? = null,
    private val lifecycleOwner: LifecycleOwner,
    private val selectedNotes: MutableLiveData<ArrayList<Note>?>,
    private val dateUtil: DateUtil,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Note>() {

        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }

    }
    private val differ = AsyncListDiffer(this, DIFF_CALLBACK)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return NoteViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.layout_note_list_item,
                parent,
                false
            ),
            interaction,
            lifecycleOwner,
            selectedNotes,
            dateUtil
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NoteViewHolder -> {
                holder.bind(differ.currentList.get(position))
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submitList(list: List<Note>) {
        val commitCallback = Runnable {
            // if process died must restore list position
            // very annoying
            interaction?.restoreListPosition()
        }
        printLogD("listadapter", "size: ${list.size}")
        differ.submitList(list, commitCallback)
    }

    fun getNote(index: Int): Note? {
        return try {
            differ.currentList[index]
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
            null
        }
    }

    class NoteViewHolder(
        itemView: View,
        private val interaction: Interaction?,
        private val lifecycleOwner: LifecycleOwner,
        private val selectedNotes: MutableLiveData<ArrayList<Note>?>,
        private val dateUtil: DateUtil,
    ) : RecyclerView.ViewHolder(itemView) {


        private val COLOR_GREY = R.color.default_grey
        private val COLOR_PRIMARY = R.color.colorPrimary
        private lateinit var note: Note

        fun bind(item: Note) = with(itemView) {
            val note_title = itemView.findViewById<TextView>(R.id.note_title)
            val note_timestamp = itemView.findViewById<TextView>(R.id.note_timestamp)

            setOnClickListener {
                interaction?.onItemSelected(adapterPosition, note)
            }
            setOnLongClickListener {
                interaction?.activateMultiSelectionMode()
                interaction?.onItemSelected(adapterPosition, note)
                true
            }
            note = item
            note_title.text = item.title
            note_timestamp.text = dateUtil.removeTimeFromDateString(item.updated_at)

            selectedNotes.observe(lifecycleOwner, Observer { notes ->

                if (notes != null) {
                    if (notes.contains(note)) {
                        note_timestamp.setTextColor(Color.parseColor("#ffffff"))
                        itemView.changeColor(R.color.colorAccent)
                    } else {
                        note_timestamp.setTextColor(Color.parseColor("#686868"))
                        changeColor(
                            newColor = COLOR_PRIMARY
                        )
                    }
                } else {
                    note_timestamp.setTextColor(Color.parseColor("#686868"))
                    changeColor(
                        newColor = COLOR_PRIMARY
                    )
                }
            })
        }
    }

    interface Interaction {

        fun onItemSelected(position: Int, item: Note)

        fun restoreListPosition()

        fun isMultiSelectionModeEnabled(): Boolean

        fun activateMultiSelectionMode()

        fun isNoteSelected(note: Note): Boolean
    }
}