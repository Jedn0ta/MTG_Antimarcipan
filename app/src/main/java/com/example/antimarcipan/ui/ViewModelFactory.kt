package com.example.antimarcipan.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.antimarcipan.MtgApp
import com.example.antimarcipan.advisor.AdvisorViewModel
import com.example.antimarcipan.collection.CollectionViewModel
import com.example.antimarcipan.decks.DeckViewModel
import com.example.antimarcipan.manual.ManualAddViewModel
import com.example.antimarcipan.scan.ScanViewModel
import com.example.antimarcipan.settings.SettingsViewModel
import com.example.antimarcipan.synergy.SynergyViewModel

private fun CreationExtras.app(): MtgApp = (this[APPLICATION_KEY] as MtgApp)

/** Central factory wiring repositories into every ViewModel. */
val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        ScanViewModel(
            app().container.collectionRepository,
            app().container.deckRepository,
            app().container.appSettings,
        )
    }
    initializer {
        CollectionViewModel(app().container.collectionRepository, app().container.deckRepository)
    }
    initializer {
        DeckViewModel(
            app().container.deckRepository,
            app().container.collectionRepository,
            app().applicationContext,
        )
    }
    initializer {
        SynergyViewModel(app().container.synergyRepository, app().container.deckRepository)
    }
    initializer {
        AdvisorViewModel(app().container.deckRepository)
    }
    initializer {
        SettingsViewModel(
            app().container.updateRepository,
            app().container.backupRepository,
            app().container.appSettings,
        )
    }
    initializer {
        ManualAddViewModel(app().container.collectionRepository, app().container.deckRepository)
    }
}
