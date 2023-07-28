package com.app.capturenotes.framework.presentation.notedetail

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.app.capturenotes.R
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.state.*
import com.app.capturenotes.business.interactors.common.DeleteNote.Companion.DELETE_ARE_YOU_SURE
import com.app.capturenotes.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_SUCCESS
import com.app.capturenotes.business.interactors.notedetail.UpdateNote.Companion.UPDATE_NOTE_FAILED_PK
import com.app.capturenotes.business.interactors.notedetail.UpdateNote.Companion.UPDATE_NOTE_SUCCESS
import com.app.capturenotes.databinding.FragmentNoteDetailBinding
import com.app.capturenotes.framework.presentation.common.*
import com.app.capturenotes.framework.presentation.notedetail.state.CollapsingToolbarState
import com.app.capturenotes.framework.presentation.notedetail.state.NoteDetailStateEvent
import com.app.capturenotes.framework.presentation.notedetail.state.NoteDetailViewState
import com.app.capturenotes.framework.presentation.notedetail.state.NoteInteractionState
import com.app.capturenotes.framework.presentation.notelist.NOTE_PENDING_DELETE_BUNDLE_KEY
import com.google.android.material.appbar.AppBarLayout
import com.yydcdut.markdown.MarkdownProcessor
import com.yydcdut.markdown.syntax.edit.EditFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

const val NOTE_DETAIL_STATE_BUNDLE_KEY =
    "com.app.capturenotes.notes.framework.presentation.notedetail.state"

@FlowPreview
@ExperimentalCoroutinesApi
class NoteDetailFragment
constructor(
    private val viewModelFactory: ViewModelProvider.Factory,
) : BaseNoteFragment(R.layout.fragment_note_detail) {

    val viewModel by viewModels<NoteDetailViewModel> {
        viewModelFactory
    }

    private var binding: FragmentNoteDetailBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setupChannel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNoteDetailBinding.bind(view)

        setupUI()
        setupOnBackPressDispatcher()
        subscribeObservers()


        binding?.noteTitle?.setOnClickListener {
            onClick_noteTitle()
        }

        binding?.noteBody?.setOnClickListener {
            onClick_noteBody()
        }

        setupMarkdown()
        getSelectedNoteFromPreviousFragment()
        restoreInstanceState()
    }

    private fun onErrorRetrievingNoteFromPreviousFragment() {
        viewModel.setStateEvent(
            NoteDetailStateEvent.CreateStateMessageEvent(
                stateMessage = StateMessage(
                    response = Response(
                        message = NOTE_DETAIL_ERROR_RETRIEVEING_SELECTED_NOTE,
                        uiComponentType = UIComponentType.Dialog(),
                        messageType = MessageType.Error()
                    )
                )
            )
        )
    }

    private fun setupMarkdown() {
        activity?.run {
            val markdownProcessor = MarkdownProcessor(requireContext())
            markdownProcessor.factory(EditFactory.create())
            markdownProcessor.live(binding?.noteBody)
        }
    }

    private fun onClick_noteTitle() {
        if (!viewModel.isEditingTitle()) {
            updateBodyInViewModel()
            updateNote()
            viewModel.setNoteInteractionTitleState(NoteInteractionState.EditState())
        }
    }

    private fun onClick_noteBody() {
        if (!viewModel.isEditingBody()) {
            updateTitleInViewModel()
            updateNote()
            viewModel.setNoteInteractionBodyState(NoteInteractionState.EditState())
        }
    }

    private fun onBackPressed() {
        view?.hideKeyboard()
        if (viewModel.checkEditState()) {
            updateBodyInViewModel()
            updateTitleInViewModel()
            updateNote()
            viewModel.exitEditState()
            displayDefaultToolbar()
        } else {
            findNavController().popBackStack()
        }
    }

    override fun onPause() {
        super.onPause()
        updateTitleInViewModel()
        updateBodyInViewModel()
        updateNote()
    }

    private fun subscribeObservers() {

        viewModel.viewState.observe(viewLifecycleOwner, Observer { viewState ->

            if (viewState != null) {

                viewState.note?.let { note ->
                    setNoteTitle(note.title)
                    setNoteBody(note.body)
                }
            }
        })

        viewModel.shouldDisplayProgressBar.observe(viewLifecycleOwner, Observer {
            uiController.displayProgressBar(it)
        })

        viewModel.stateMessage.observe(viewLifecycleOwner, Observer { stateMessage ->

            stateMessage?.response?.let { response ->

                when (response.message) {

                    UPDATE_NOTE_SUCCESS -> {
                        viewModel.setIsUpdatePending(false)
                        viewModel.clearStateMessage()
                    }

                    DELETE_NOTE_SUCCESS -> {
                        viewModel.clearStateMessage()
                        onDeleteSuccess()
                    }

                    else -> {
                        uiController.onResponseReceived(
                            response = stateMessage.response,
                            stateMessageCallback = object : StateMessageCallback {
                                override fun removeMessageFromStack() {
                                    viewModel.clearStateMessage()
                                }
                            }
                        )
                        when (response.message) {
                            UPDATE_NOTE_FAILED_PK -> {
                                findNavController().popBackStack()
                            }
                            NOTE_DETAIL_ERROR_RETRIEVEING_SELECTED_NOTE -> {
                                findNavController().popBackStack()
                            }
                            else -> {
                                // do nothing
                            }
                        }
                    }
                }
            }

        })

        viewModel.collapsingToolbarState.observe(viewLifecycleOwner, Observer { state ->

            when (state) {

                is CollapsingToolbarState.Expanded -> {
                    transitionToExpandedMode()
                }

                is CollapsingToolbarState.Collapsed -> {
                    transitionToCollapsedMode()
                }
            }
        })

        viewModel.noteTitleInteractionState.observe(viewLifecycleOwner, Observer { state ->

            when (state) {

                is NoteInteractionState.EditState -> {
                    binding?.noteTitle?.enableContentInteraction()
                    view?.showKeyboard()
                    displayEditStateToolbar()
                    viewModel.setIsUpdatePending(true)
                }

                is NoteInteractionState.DefaultState -> {
                    binding?.noteTitle?.disableContentInteraction()
                }
            }
        })

        viewModel.noteBodyInteractionState.observe(viewLifecycleOwner, Observer { state ->

            when (state) {

                is NoteInteractionState.EditState -> {
                    binding?.noteBody?.enableContentInteraction()
                    view?.showKeyboard()
                    displayEditStateToolbar()
                    viewModel.setIsUpdatePending(true)
                }

                is NoteInteractionState.DefaultState -> {
                    binding?.noteBody?.disableContentInteraction()
                }
            }
        })
    }

    private fun displayDefaultToolbar() {
        activity?.let { a ->
            binding?.toolBar?.findViewById<ImageView>(R.id.toolbar_primary_icon)?.setImageDrawable(
                resources.getDrawable(
                    R.drawable.ic_arrow_back_grey_24dp,
                    a.application.theme
                )
            )
            binding?.toolBar?.findViewById<ImageView>(R.id.toolbar_secondary_icon)?.setImageDrawable(
                resources.getDrawable(
                    R.drawable.ic_delete,
                    a.application.theme
                )
            )
        }
    }

    private fun displayEditStateToolbar() {
        activity?.let { a ->
            binding?.toolBar?.findViewById<ImageView>(R.id.toolbar_primary_icon)?.setImageDrawable(
                resources.getDrawable(
                    R.drawable.ic_close_grey_24dp,
                    a.application.theme
                )
            )
            binding?.toolBar?.findViewById<ImageView>(R.id.toolbar_secondary_icon)?.setImageDrawable(
                resources.getDrawable(
                    R.drawable.ic_done_grey_24dp,
                    a.application.theme
                )
            )
        }
    }

    private fun setNoteTitle(title: String) {
        binding?.noteTitle?.setText(title)
    }

    private fun getNoteTitle(): String {
        return binding?.noteTitle?.text.toString()
    }

    private fun getNoteBody(): String {
        return binding?.noteBody?.text.toString()
    }

    private fun setNoteBody(body: String?) {
        binding?.noteBody?.setText(body)
    }

    private fun getSelectedNoteFromPreviousFragment() {
        arguments?.let { args ->
            (args.getParcelable(NOTE_DETAIL_SELECTED_NOTE_BUNDLE_KEY) as Note?)?.let { selectedNote ->
                viewModel.setNote(selectedNote)
            } ?: onErrorRetrievingNoteFromPreviousFragment()
        }
    }

    private fun restoreInstanceState() {
        arguments?.let { args ->
            (args.getParcelable(NOTE_DETAIL_STATE_BUNDLE_KEY) as NoteDetailViewState?)?.let { viewState ->
                viewModel.setViewState(viewState)
                // One-time check after rotation
                if (viewModel.isToolbarCollapsed()) {
                    binding?.appBar?.setExpanded(false)
                    transitionToCollapsedMode()
                } else {
                    binding?.appBar?.setExpanded(true)
                    transitionToExpandedMode()
                }
            }
        }
    }

    private fun updateTitleInViewModel() {
        if (viewModel.isEditingTitle()) {
            viewModel.updateNoteTitle(getNoteTitle())
        }
    }

    private fun updateBodyInViewModel() {
        if (viewModel.isEditingBody()) {
            viewModel.updateNoteBody(getNoteBody())
        }
    }

    private fun setupUI() {
        binding?.noteTitle?.disableContentInteraction()
        binding?.noteBody?.disableContentInteraction()
        displayDefaultToolbar()
        transitionToExpandedMode()


        binding?.appBar?.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, offset ->
                if (offset < COLLAPSING_TOOLBAR_VISIBILITY_THRESHOLD) {
                    updateTitleInViewModel()
                    if (viewModel.isEditingTitle()) {
                        viewModel.exitEditState()
                        displayDefaultToolbar()
                        updateNote()
                    }
                    viewModel.setCollapsingToolbarState(CollapsingToolbarState.Collapsed())
                } else {
                    viewModel.setCollapsingToolbarState(CollapsingToolbarState.Expanded())
                }
            })

        binding?.toolBar?.findViewById<ImageView>(R.id.toolbar_primary_icon)?.setOnClickListener {
            if (viewModel.checkEditState()) {
                view?.hideKeyboard()
                viewModel.triggerNoteObservers()
                viewModel.exitEditState()
                displayDefaultToolbar()
            } else {
                onBackPressed()
            }
        }

        binding?.toolBar?.findViewById<ImageView>(R.id.toolbar_secondary_icon)?.setOnClickListener {
            if (viewModel.checkEditState()) {
                view?.hideKeyboard()
                updateTitleInViewModel()
                updateBodyInViewModel()
                updateNote()
                viewModel.exitEditState()
                displayDefaultToolbar()
            } else {
                deleteNote()
            }
        }
    }

    private fun deleteNote() {
        viewModel.setStateEvent(
            NoteDetailStateEvent.CreateStateMessageEvent(
                stateMessage = StateMessage(
                    response = Response(
                        message = DELETE_ARE_YOU_SURE,
                        uiComponentType = UIComponentType.AreYouSureDialog(
                            object : AreYouSureCallback {
                                override fun proceed() {
                                    viewModel.getNote()?.let { note ->
                                        initiateDeleteTransaction(note)
                                    }
                                }

                                override fun cancel() {
                                    // do nothing
                                }
                            }
                        ),
                        messageType = MessageType.Info()
                    )
                )
            )
        )
    }

    private fun initiateDeleteTransaction(note: Note) {
        viewModel.beginPendingDelete(note)
    }

    private fun onDeleteSuccess() {
        val bundle = bundleOf(NOTE_PENDING_DELETE_BUNDLE_KEY to viewModel.getNote())
        viewModel.setNote(null) // clear note from ViewState
        viewModel.setIsUpdatePending(false) // prevent update onPause
        findNavController().navigate(
            R.id.action_note_detail_fragment_to_noteListFragment,
            bundle
        )
    }

    private fun setupOnBackPressDispatcher() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun updateNote() {
        if (viewModel.getIsUpdatePending()) {
            viewModel.setStateEvent(
                NoteDetailStateEvent.UpdateNoteEvent()
            )
        }
    }

    private fun transitionToCollapsedMode() {
        binding?.noteTitle?.fadeOut()
        binding?.toolBar?.findViewById<TextView>(R.id.tool_bar_title)?.let {
            displayToolbarTitle(it, getNoteTitle(), true)
        }
    }

    private fun transitionToExpandedMode() {
        binding?.noteTitle?.fadeIn()
        binding?.toolBar?.findViewById<TextView>(R.id.tool_bar_title)
            ?.let {
                displayToolbarTitle(it, null, true)
            }
    }

    override fun inject() {
        getAppComponent().inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val viewState = viewModel.getCurrentViewStateOrNew()
        outState.putParcelable(NOTE_DETAIL_STATE_BUNDLE_KEY, viewState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}