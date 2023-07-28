package com.app.capturenotes.framework.presentation.notelist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.app.capturenotes.R
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.state.*
import com.app.capturenotes.business.domain.util.DateUtil
import com.app.capturenotes.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_PENDING
import com.app.capturenotes.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_SUCCESS
import com.app.capturenotes.business.interactors.notelist.DeleteMultipleNotes.Companion.DELETE_NOTES_ARE_YOU_SURE
import com.app.capturenotes.databinding.FragmentNoteListBinding
import com.app.capturenotes.framework.datasource.cache.database.NOTE_FILTER_DATE_CREATED
import com.app.capturenotes.framework.datasource.cache.database.NOTE_FILTER_TITLE
import com.app.capturenotes.framework.datasource.cache.database.NOTE_ORDER_ASC
import com.app.capturenotes.framework.datasource.cache.database.NOTE_ORDER_DESC
import com.app.capturenotes.framework.presentation.common.BaseNoteFragment
import com.app.capturenotes.framework.presentation.common.TopSpacingItemDecoration
import com.app.capturenotes.framework.presentation.common.hideKeyboard
import com.app.capturenotes.framework.presentation.notedetail.NOTE_DETAIL_SELECTED_NOTE_BUNDLE_KEY
import com.app.capturenotes.framework.presentation.notelist.state.NoteListStateEvent
import com.app.capturenotes.framework.presentation.notelist.state.NoteListToolbarState
import com.app.capturenotes.framework.presentation.notelist.state.NoteListViewState
import com.app.capturenotes.util.AndroidTestUtils
import com.app.capturenotes.util.TodoCallback
import com.app.capturenotes.util.printLogD
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.*
import javax.inject.Inject


const val NOTE_LIST_STATE_BUNDLE_KEY =
    "com.app.capturenotes.notes.framework.presentation.notelist.state"


@FlowPreview
@ExperimentalCoroutinesApi
class NoteListFragment
constructor(
    private val viewModelFactory: ViewModelProvider.Factory,
    private val dateUtil: DateUtil,
) : BaseNoteFragment(R.layout.fragment_note_list),
    NoteListAdapter.Interaction,
    ItemTouchHelperAdapter {

    @Inject
    lateinit var androidTestUtils: AndroidTestUtils

    var binding: FragmentNoteListBinding? = null

    val viewModel: NoteListViewModel by viewModels {
        viewModelFactory
    }

    private var listAdapter: NoteListAdapter? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    lateinit var intentSenderLauncher: ActivityResultLauncher<Intent>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        intentSenderLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                Toast.makeText(
                    requireContext(),
                    "Logged in with " + GoogleSignIn.getLastSignedInAccount(
                        requireContext()
                    )?.email,
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setupChannel()
        arguments?.let { args ->
            args.getParcelable<Note>(NOTE_PENDING_DELETE_BUNDLE_KEY)?.let { note ->
                viewModel.setNotePendingDelete(note)
                showUndoSnackbar_deleteNote()
                clearArgs()
            }
        }
    }

    private fun clearArgs() {
        arguments?.clear()
    }

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentNoteListBinding.bind(view)
        setupUI()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFAB()
        subscribeObservers()

        restoreInstanceState(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        viewModel.retrieveNumNotesInCache()
        viewModel.clearList()
        viewModel.refreshSearchQuery()
    }

    override fun onPause() {
        super.onPause()
        saveLayoutManagerState()
    }

    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { inState ->
            (inState[NOTE_LIST_STATE_BUNDLE_KEY] as NoteListViewState?)?.let { viewState ->
                viewModel.setViewState(viewState)
            }
        }
    }

    // Why didn't I use the "SavedStateHandle" here?
    // It sucks and doesn't work for testing
    override fun onSaveInstanceState(outState: Bundle) {
        val viewState = viewModel.viewState.value

        //clear the list. Don't want to save a large list to bundle.
        viewState?.noteList = ArrayList()

        outState.putParcelable(
            NOTE_LIST_STATE_BUNDLE_KEY,
            viewState
        )
        super.onSaveInstanceState(outState)
    }

    override fun restoreListPosition() {
        viewModel.getLayoutManagerState()?.let { lmState ->
            binding?.recyclerView?.layoutManager?.onRestoreInstanceState(lmState)
        }
    }

    private fun saveLayoutManagerState() {
        binding?.recyclerView?.layoutManager?.onSaveInstanceState()?.let { lmState ->
            viewModel.setLayoutManagerState(lmState)
        }
    }

    private fun setupRecyclerView() {
        binding?.recyclerView?.apply {
            layoutManager = LinearLayoutManager(activity)
            val topSpacingDecorator = TopSpacingItemDecoration(20)
            addItemDecoration(topSpacingDecorator)
            itemTouchHelper = ItemTouchHelper(
                NoteItemTouchHelperCallback(
                    this@NoteListFragment,
                    viewModel.noteListInteractionManager
                )
            )
            listAdapter = NoteListAdapter(
                this@NoteListFragment,
                viewLifecycleOwner,
                viewModel.noteListInteractionManager.selectedNotes,
                dateUtil
            )
            itemTouchHelper?.attachToRecyclerView(this)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastPosition = layoutManager.findLastVisibleItemPosition()
                    if (lastPosition == listAdapter?.itemCount?.minus(1)) {
                        viewModel.nextPage()
                    }
                }
            })
            adapter = listAdapter
        }
    }

    private fun enableMultiSelectToolbarState() {
        view?.let { v ->
            val view = View.inflate(
                v.context,
                R.layout.layout_multiselection_toolbar,
                null
            )
            view.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            binding?.toolbarContentContainer?.addView(view)
            setupMultiSelectionToolbar(view)
        }
    }

    private fun setupMultiSelectionToolbar(parentView: View) {
        parentView
            .findViewById<ImageView>(R.id.action_exit_multiselect_state)
            .setOnClickListener {
                viewModel.setToolbarState(NoteListToolbarState.SearchViewState())
            }

        parentView
            .findViewById<ImageView>(R.id.action_delete_notes)
            .setOnClickListener {
                deleteNotes()
            }
    }

    private fun enableSearchViewToolbarState() {
        view?.let { v ->
            val view = View.inflate(
                v.context,
                R.layout.layout_searchview_toolbar,
                null
            )
            view.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            binding?.toolbarContentContainer?.addView(view)
            setupSearchView()
            setupFilterButton()
            setupLoginButton()
        }
    }

    private fun disableMultiSelectToolbarState() {
        view?.let {
            val view =
                binding?.toolbarContentContainer?.findViewById<Toolbar>(R.id.multiselect_toolbar)
            binding?.toolbarContentContainer?.removeView(view)
            viewModel.clearSelectedNotes()
        }
    }

    private fun disableSearchViewToolbarState() {
        view?.let {
            val view =
                binding?.toolbarContentContainer?.findViewById<Toolbar>(R.id.searchview_toolbar)
            binding?.toolbarContentContainer?.removeView(view)
        }
    }

    override fun isMultiSelectionModeEnabled() = viewModel.isMultiSelectionStateActive()

    override fun activateMultiSelectionMode() =
        viewModel.setToolbarState(NoteListToolbarState.MultiSelectionState())

    private fun subscribeObservers() {

        viewModel.toolbarState.observe(viewLifecycleOwner, Observer { toolbarState ->

            when (toolbarState) {

                is NoteListToolbarState.MultiSelectionState -> {
                    enableMultiSelectToolbarState()
                    disableSearchViewToolbarState()
                }

                is NoteListToolbarState.SearchViewState -> {
                    enableSearchViewToolbarState()
                    disableMultiSelectToolbarState()
                }
            }
        })

        viewModel.viewState.observe(viewLifecycleOwner, Observer { viewState ->

            if (viewState != null) {
                viewState.noteList?.let { noteList ->
                    if (viewModel.isPaginationExhausted()
                        && !viewModel.isQueryExhausted()
                    ) {
                        viewModel.setQueryExhausted(true)
                    }
                    listAdapter?.submitList(noteList)
                    listAdapter?.notifyDataSetChanged()
                }

                // a note been inserted or selected
                viewState.newNote?.let { newNote ->
                    navigateToDetailFragment(newNote)
                }

            }
        })

        viewModel.shouldDisplayProgressBar.observe(viewLifecycleOwner, Observer {
            printActiveJobs()
            uiController.displayProgressBar(it)
        })

        viewModel.stateMessage.observe(viewLifecycleOwner, Observer { stateMessage ->
            stateMessage?.let { message ->
                if (message.response.message?.equals(DELETE_NOTE_SUCCESS) == true) {
                    showUndoSnackbar_deleteNote()
                } else {
                    uiController.onResponseReceived(
                        response = message.response,
                        stateMessageCallback = object : StateMessageCallback {
                            override fun removeMessageFromStack() {
                                viewModel.clearStateMessage()
                            }
                        }
                    )
                }
            }
        })
    }

    private fun showUndoSnackbar_deleteNote() {
        uiController.onResponseReceived(
            response = Response(
                message = DELETE_NOTE_PENDING,
                uiComponentType = UIComponentType.SnackBar(
                    undoCallback = object : SnackbarUndoCallback {
                        override fun undo() {
                            viewModel.undoDelete()
                        }
                    },
                    onDismissCallback = object : TodoCallback {
                        override fun execute() {
                            // if the note is not restored, clear pending note
                            viewModel.setNotePendingDelete(null)
                        }
                    }
                ),
                messageType = MessageType.Info()
            ),
            stateMessageCallback = object : StateMessageCallback {
                override fun removeMessageFromStack() {
                    viewModel.clearStateMessage()
                }
            }
        )
    }

    // for debugging
    private fun printActiveJobs() {

        for ((index, job) in viewModel.getActiveJobs().withIndex()) {
            printLogD("NoteList",
                "${index}: ${job}")
        }
    }

    private fun navigateToDetailFragment(selectedNote: Note) {
        val bundle = bundleOf(NOTE_DETAIL_SELECTED_NOTE_BUNDLE_KEY to selectedNote)
        findNavController().navigate(
            R.id.action_note_list_fragment_to_noteDetailFragment,
            bundle
        )
        viewModel.setNote(null)
    }

    private fun setupUI() {
        view?.hideKeyboard()
    }

    override fun inject() {
        getAppComponent().inject(this)
    }

    override fun onItemSelected(position: Int, item: Note) {
        if (isMultiSelectionModeEnabled()) {
            viewModel.addOrRemoveNoteFromSelectedList(item)
        } else {
            viewModel.setNote(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listAdapter = null // can leak memory
        itemTouchHelper = null // can leak memory
    }

    override fun isNoteSelected(note: Note): Boolean {
        return viewModel.isNoteSelected(note)
    }

    override fun onItemSwiped(position: Int) {
        if (!viewModel.isDeletePending()) {
            listAdapter?.getNote(position)?.let { note ->
                viewModel.beginPendingDelete(note)
            }
        } else {
            listAdapter?.notifyDataSetChanged()
        }
    }

    private fun deleteNotes() {
        viewModel.setStateEvent(
            NoteListStateEvent.CreateStateMessageEvent(
                stateMessage = StateMessage(
                    response = Response(
                        message = DELETE_NOTES_ARE_YOU_SURE,
                        uiComponentType = UIComponentType.AreYouSureDialog(
                            object : AreYouSureCallback {
                                override fun proceed() {
                                    viewModel.deleteNotes()
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

    private fun setupSearchView() {

        val searchViewToolbar: Toolbar? =
            binding?.toolbarContentContainer?.findViewById(R.id.searchview_toolbar)

        searchViewToolbar?.let { toolbar ->

            val searchView = toolbar.findViewById<SearchView>(R.id.search_view)

            val searchPlate: SearchView.SearchAutoComplete? =
                searchView.findViewById(androidx.appcompat.R.id.search_src_text)

            // can't use QueryTextListener in production b/c can't submit an empty string
            when {
                androidTestUtils.isTest() -> {
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            viewModel.setQuery(query)
                            startNewSearch()
                            return true
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            return true
                        }

                    })
                }

                else -> {
                    searchPlate?.setOnEditorActionListener { v, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED
                            || actionId == EditorInfo.IME_ACTION_SEARCH
                        ) {
                            val searchQuery = v.text.toString()
                            viewModel.setQuery(searchQuery)
                            startNewSearch()
                        }
                        true
                    }
                }
            }
        }
    }

    private fun setupFAB() {
        binding?.addNewNoteFab?.setOnClickListener {
            uiController.displayInputCaptureDialog(
                getString(R.string.text_enter_a_title),
                object : DialogInputCaptureCallback {
                    override fun onTextCaptured(text: String) {
                        val newNote = viewModel.createNewNote(title = text)
                        viewModel.setStateEvent(
                            NoteListStateEvent.InsertNewNoteEvent(
                                title = newNote.title
                            )
                        )
                        lifecycleScope.launch {
                            delay(1000L)
                            startNewSearch()
                        }
                    }
                }
            )
        }
    }

    private fun startNewSearch() {
        printLogD("DCM", "start new search")
        viewModel.clearList()
        viewModel.loadFirstPage()
    }

    private fun setupSwipeRefresh() {
        binding?.swipeRefresh?.setOnRefreshListener {
            startNewSearch()
            binding?.swipeRefresh?.isRefreshing = false
        }
    }


    private fun setupLoginButton() {
        val searchViewToolbar: Toolbar? =
            binding?.toolbarContentContainer?.findViewById(R.id.searchview_toolbar)
        searchViewToolbar?.findViewById<ImageView>(R.id.action_login)?.setOnClickListener {
            showLoginDialog()
        }
    }

    private fun setupFilterButton() {
        val searchViewToolbar: Toolbar? =
            binding?.toolbarContentContainer?.findViewById(R.id.searchview_toolbar)
        searchViewToolbar?.findViewById<ImageView>(R.id.action_filter)?.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showLoginDialog() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        val gclient = GoogleSignIn.getClient(requireContext(), gso)

        if (GoogleSignIn.getLastSignedInAccount(requireContext()) != null) {
            MaterialDialog(requireContext()).show {
                title(null, "Sign-Out")
                message(null, "Confirm you want to Sign-Out of your Drive Storage Account ?")
                positiveButton(null, "OK") {
                    gclient.signOut().addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(requireContext(), "Logged Out", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                negativeButton(null, "Cancel")
            }
        } else {
            MaterialDialog(requireContext()).show {
                title(null, "Sign-In")
                message(null,
                    "Sign-in with your google account to sync your notes across multiple devices")
                positiveButton(null, "OK") {
                    intentSenderLauncher.launch(gclient.signInIntent)
                }
                negativeButton(null, "Cancel")
            }
        }
    }

    fun showFilterDialog() {

        activity?.let {
            val dialog = MaterialDialog(it)
                .noAutoDismiss()
                .customView(R.layout.layout_filter)

            val view = dialog.getCustomView()

            val filter = viewModel.getFilter()
            val order = viewModel.getOrder()

            view.findViewById<RadioGroup>(R.id.filter_group).apply {
                when (filter) {
                    NOTE_FILTER_DATE_CREATED -> check(R.id.filter_date)
                    NOTE_FILTER_TITLE -> check(R.id.filter_title)
                }
            }

            view.findViewById<RadioGroup>(R.id.order_group).apply {
                when (order) {
                    NOTE_ORDER_ASC -> check(R.id.filter_asc)
                    NOTE_ORDER_DESC -> check(R.id.filter_desc)
                }
            }

            view.findViewById<TextView>(R.id.positive_button).setOnClickListener {

                val newFilter =
                    when (view.findViewById<RadioGroup>(R.id.filter_group).checkedRadioButtonId) {
                        R.id.filter_title -> NOTE_FILTER_TITLE
                        R.id.filter_date -> NOTE_FILTER_DATE_CREATED
                        else -> NOTE_FILTER_DATE_CREATED
                    }

                val newOrder =
                    when (view.findViewById<RadioGroup>(R.id.order_group).checkedRadioButtonId) {
                        R.id.filter_desc -> "-"
                        else -> ""
                    }

                viewModel.apply {
                    saveFilterOptions(newFilter, newOrder)
                    setNoteFilter(newFilter)
                    setNoteOrder(newOrder)
                }

                startNewSearch()

                dialog.dismiss()
            }

            view.findViewById<TextView>(R.id.negative_button).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}