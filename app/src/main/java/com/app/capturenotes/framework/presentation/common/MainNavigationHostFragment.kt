package com.app.instastorytale.framework.presentation.ui.main.fragments.NavigationHostFragment

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentFactory
import androidx.navigation.fragment.NavHostFragment
import com.app.capturenotes.framework.presentation.BaseApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MainNavigationHostFragment : NavHostFragment() {

    @Inject
    lateinit var  fragmentFactory: FragmentFactory

    override fun onAttach(context: Context) {
        super.onAttach(context)
        childFragmentManager.fragmentFactory=fragmentFactory
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        (activity.application as BaseApplication).appComponent
            .inject(this)
    }
}