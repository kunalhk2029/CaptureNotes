package com.app.capturenotes.framework.presentation.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.app.capturenotes.R
import com.app.capturenotes.framework.presentation.common.BaseNoteFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject
import javax.inject.Singleton


@FlowPreview
@ExperimentalCoroutinesApi
@Singleton
class SplashFragment
@Inject
constructor(
    private val viewModelFactory: ViewModelProvider.Factory,
) : BaseNoteFragment(R.layout.fragment_splash) {

    val viewModel: SplashViewModel by viewModels {
        viewModelFactory
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkFirebaseAuth()
    }

    private fun checkFirebaseAuth() {
        if (GoogleSignIn.getLastSignedInAccount(requireContext()) != null) {
            subscribeObservers()
        } else {
            navNoteListFragment()
        }
    }

    private fun subscribeObservers() {
        viewModel.hasSyncBeenExecuted()
            .observe(viewLifecycleOwner) { hasSyncBeenExecuted ->
                if (hasSyncBeenExecuted) {
                    navNoteListFragment()
                }
            }
    }

    private fun navNoteListFragment() {
        findNavController().navigate(R.id.action_splashFragment_to_noteListFragment)
    }

    override fun inject() {
        getAppComponent().inject(this)
    }
}