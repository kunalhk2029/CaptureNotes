package com.app.capturenotes.di

import com.app.capturenotes.framework.presentation.BaseApplication
import com.app.capturenotes.framework.presentation.MainActivity
import com.app.capturenotes.framework.presentation.notedetail.NoteDetailFragment
import com.app.capturenotes.framework.presentation.notelist.NoteListFragment
import com.app.capturenotes.framework.presentation.splash.NoteNetworkSyncManager
import com.app.capturenotes.framework.presentation.splash.SplashFragment
import com.app.instastorytale.framework.presentation.ui.main.fragments.NavigationHostFragment.MainNavigationHostFragment
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@FlowPreview
@Singleton
@Component(
    modules = [
        ProductionModule::class,
        AppModule::class,
        NoteViewModelModule::class,
        NoteViewModelModule::class,
        NoteFragmentFactoryModule::class,
    ]
)
interface AppComponent {

    val noteNetworkSync: NoteNetworkSyncManager

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance app: BaseApplication): AppComponent
    }

    fun inject(mainActivity: MainActivity)

    fun inject(splashFragment: SplashFragment)

    fun inject(noteListFragment: NoteListFragment)

    fun inject(mainNavigationHostFragment: MainNavigationHostFragment)

    fun inject(noteDetailFragment: NoteDetailFragment)
}