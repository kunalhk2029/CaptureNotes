package com.app.capturenotes.di

import com.app.capturenotes.framework.datasource.cache.NoteDaoServiceTests
import com.app.capturenotes.framework.datasource.network.NoteFirestoreServiceTests
import com.app.capturenotes.framework.presentation.BaseApplication
import com.app.capturenotes.framework.presentation.MainActivity
import com.app.capturenotes.framework.presentation.TestBaseApplication
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
        TestModule::class,
        AppModule::class,
        TestNoteFragmentFactoryModule::class,
        NoteViewModelModule::class,
    ]
)
interface TestAppComponent:AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance app: BaseApplication): TestAppComponent
    }

    fun inject(noteDaoServiceTests: NoteDaoServiceTests)

    fun inject(noteFirestoreServiceTests: NoteFirestoreServiceTests)

}












