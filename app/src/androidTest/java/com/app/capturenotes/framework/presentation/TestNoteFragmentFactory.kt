package com.app.capturenotes.framework.presentation

import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import com.app.capturenotes.business.domain.util.DateUtil
import com.app.capturenotes.framework.presentation.notedetail.NoteDetailFragment
import com.app.capturenotes.framework.presentation.notelist.NoteListFragment
import com.app.capturenotes.framework.presentation.splash.SplashFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@FlowPreview
@Singleton
class TestNoteFragmentFactory
@Inject
constructor(
    private val viewModelFactory: ViewModelProvider.Factory,
    private val dateUtil: DateUtil,
) : FragmentFactory() {

    lateinit var uiController: UIController

    override fun instantiate(classLoader: ClassLoader, className: String) =

        when (className) {

            NoteListFragment::class.java.name -> {
                val fragment = NoteListFragment(viewModelFactory, dateUtil)
                if (::uiController.isInitialized) {
                    fragment.setUIController(uiController)
                }
                fragment
            }

            NoteDetailFragment::class.java.name -> {
                val fragment = NoteDetailFragment(viewModelFactory)
                if (::uiController.isInitialized) {
                    fragment.setUIController(uiController)
                }
                fragment
            }

            SplashFragment::class.java.name -> {
                val fragment = SplashFragment(viewModelFactory)
                if (::uiController.isInitialized) {
                    fragment.setUIController(uiController)
                }
                fragment
            }

            else -> {
                super.instantiate(classLoader, className)
            }
        }

}