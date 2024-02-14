package org.thoughtcrime.securesms.components.settings.app.chats

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.util.BackupUtil
import org.thoughtcrime.securesms.util.ConversationUtil
import org.thoughtcrime.securesms.util.TextSecurePreferences // JW: added
import org.thoughtcrime.securesms.util.ThrottledDebouncer
import org.thoughtcrime.securesms.util.livedata.Store

class ChatsSettingsViewModel @JvmOverloads constructor(
  private val repository: ChatsSettingsRepository = ChatsSettingsRepository()
) : ViewModel() {

  private val refreshDebouncer = ThrottledDebouncer(500L)

  private val store: Store<ChatsSettingsState> = Store(
    ChatsSettingsState(
      generateLinkPreviews = SignalStore.settings().isLinkPreviewsEnabled,
      useAddressBook = SignalStore.settings().isPreferSystemContactPhotos,
      keepMutedChatsArchived = SignalStore.settings().shouldKeepMutedChatsArchived(),
      useSystemEmoji = SignalStore.settings().isPreferSystemEmoji,
      enterKeySends = SignalStore.settings().isEnterKeySends,
      chatBackupsEnabled = SignalStore.settings().isBackupEnabled && BackupUtil.canUserAccessBackupDirectory(ApplicationDependencies.getApplication())
      // JW: added
      ,
      keepViewOnceMessages = TextSecurePreferences.isKeepViewOnceMessages(ApplicationDependencies.getApplication()),
      ignoreRemoteDelete = TextSecurePreferences.isIgnoreRemoteDelete(ApplicationDependencies.getApplication()),
      deleteMediaOnly = TextSecurePreferences.isDeleteMediaOnly(ApplicationDependencies.getApplication()),
      whoCanAddYouToGroups = TextSecurePreferences.whoCanAddYouToGroups(ApplicationDependencies.getApplication())
    )
  )

  val state: LiveData<ChatsSettingsState> = store.stateLiveData

  fun setGenerateLinkPreviewsEnabled(enabled: Boolean) {
    store.update { it.copy(generateLinkPreviews = enabled) }
    SignalStore.settings().isLinkPreviewsEnabled = enabled
    repository.syncLinkPreviewsState()
  }

  fun setUseAddressBook(enabled: Boolean) {
    store.update { it.copy(useAddressBook = enabled) }
    refreshDebouncer.publish { ConversationUtil.refreshRecipientShortcuts() }
    SignalStore.settings().isPreferSystemContactPhotos = enabled
    repository.syncPreferSystemContactPhotos()
  }

  fun setKeepMutedChatsArchived(enabled: Boolean) {
    store.update { it.copy(keepMutedChatsArchived = enabled) }
    SignalStore.settings().setKeepMutedChatsArchived(enabled)
    repository.syncKeepMutedChatsArchivedState()
  }

  fun setUseSystemEmoji(enabled: Boolean) {
    store.update { it.copy(useSystemEmoji = enabled) }
    SignalStore.settings().isPreferSystemEmoji = enabled
  }

  fun setEnterKeySends(enabled: Boolean) {
    store.update { it.copy(enterKeySends = enabled) }
    SignalStore.settings().isEnterKeySends = enabled
  }

  fun refresh() {
    val backupsEnabled = SignalStore.settings().isBackupEnabled && BackupUtil.canUserAccessBackupDirectory(ApplicationDependencies.getApplication())
    if (store.state.chatBackupsEnabled != backupsEnabled) {
      store.update { it.copy(chatBackupsEnabled = backupsEnabled) }
    }
  }

  // JW: added
  fun keepViewOnceMessages(enabled: Boolean) {
    TextSecurePreferences.setKeepViewOnceMessages(ApplicationDependencies.getApplication(), enabled)
    refresh()
  }

  // JW: added
  fun ignoreRemoteDelete(enabled: Boolean) {
    TextSecurePreferences.setIgnoreRemoteDelete(ApplicationDependencies.getApplication(), enabled)
    refresh()
  }

  // JW: added
  fun deleteMediaOnly(enabled: Boolean) {
    TextSecurePreferences.setDeleteMediaOnly(ApplicationDependencies.getApplication(), enabled)
    refresh()
  }


  // JW: added
  fun setWhoCanAddYouToGroups(adder: String) {
    TextSecurePreferences.setWhoCanAddYouToGroups(ApplicationDependencies.getApplication(), adder)
    refresh()
  }

  // JW: added
  private fun getState() = ChatsSettingsState(
    generateLinkPreviews = SignalStore.settings().isLinkPreviewsEnabled,
    useAddressBook = SignalStore.settings().isPreferSystemContactPhotos,
    keepMutedChatsArchived = SignalStore.settings().shouldKeepMutedChatsArchived(),
    useSystemEmoji = SignalStore.settings().isPreferSystemEmoji,
    enterKeySends = SignalStore.settings().isEnterKeySends,
    chatBackupsEnabled = SignalStore.settings().isBackupEnabled,
    keepViewOnceMessages = TextSecurePreferences.isKeepViewOnceMessages(ApplicationDependencies.getApplication()),
    ignoreRemoteDelete = TextSecurePreferences.isIgnoreRemoteDelete(ApplicationDependencies.getApplication()),
    deleteMediaOnly = TextSecurePreferences.isDeleteMediaOnly(ApplicationDependencies.getApplication()),
    whoCanAddYouToGroups = TextSecurePreferences.whoCanAddYouToGroups(ApplicationDependencies.getApplication())
  )
}
