package com.quran.labs.androidquran.service.util

import android.content.Context
import android.content.Intent
import com.quran.data.core.QuranFileManager
import com.quran.data.core.QuranInfo
import com.quran.data.di.AppScope
import com.quran.data.model.SuraAyah
import com.quran.data.model.audio.Qari
import com.quran.labs.androidquran.common.audio.model.download.AudioDownloadMetadata
import com.quran.labs.androidquran.service.QuranDownloadService
import com.quran.labs.androidquran.util.AudioUtils
import com.quran.mobile.common.download.Downloader
import com.quran.mobile.di.qualifier.ApplicationContext
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class DownloadStarter @Inject constructor(
  @ApplicationContext private val appContext: Context,
  private val quranInfo: QuranInfo,
  private val fileManager: QuranFileManager,
  private val audioUtils: AudioUtils
) : Downloader {

  /**
   * This handles downloading a batch of suras, even if they are not contiguous.
   *
   * This is slightly less efficient than the below method, since it does more allocations.
   * In the future, we should combine them into a single one.
   */
  override fun downloadBatchSuras(qari: Qari, suras: List<Int>, downloadDatabase: Boolean) {
    val basePath = fileManager.audioFileDirectory()
    val baseUri = basePath + qari.path
    val isGapless = qari.isGapless
    val sheikhName = appContext.getString(qari.nameResource)

    val intent = ServiceIntentHelper.getDownloadIntent(
      appContext,
      audioUtils.getQariUrl(qari),
      baseUri,
      sheikhName,
      AUDIO_DOWNLOAD_KEY + qari.id + suras.first(),
      QuranDownloadService.DOWNLOAD_TYPE_AUDIO
    ).apply {
      putExtra(QuranDownloadService.EXTRA_AUDIO_BATCH, suras.toIntArray())
      putExtra(QuranDownloadService.EXTRA_IS_GAPLESS, isGapless)
      putExtra(QuranDownloadService.EXTRA_METADATA, AudioDownloadMetadata(qari.id))
      if (downloadDatabase && isGapless) {
        putExtra(QuranDownloadService.EXTRA_DOWNLOAD_DATABASE, fileManager.urlForDatabase(qari))
      }
    }
    appContext.startService(intent)
  }

  /**
   * This handles downloading of a consecutive range of suras.
   *
   * This is slightly more efficient than the above method, since it does less allocations.
   * In the future, we should combine them into a single one.
   */
  override fun downloadSuras(qari: Qari, startSura: Int, endSura: Int, downloadDatabase: Boolean) {
    val basePath = fileManager.audioFileDirectory()
    val baseUri = basePath + qari.path
    val isGapless = qari.isGapless
    val sheikhName = appContext.getString(qari.nameResource)

    val intent = ServiceIntentHelper.getDownloadIntent(
      appContext,
      audioUtils.getQariUrl(qari),
      baseUri,
      sheikhName,
      AUDIO_DOWNLOAD_KEY + qari.id + startSura,
      QuranDownloadService.DOWNLOAD_TYPE_AUDIO
    ).apply {
      putExtra(QuranDownloadService.EXTRA_START_VERSE, SuraAyah(startSura, 1))
      putExtra(QuranDownloadService.EXTRA_END_VERSE, SuraAyah(endSura, quranInfo.getNumberOfAyahs(endSura)))
      putExtra(QuranDownloadService.EXTRA_IS_GAPLESS, isGapless)
      putExtra(QuranDownloadService.EXTRA_METADATA, AudioDownloadMetadata(qari.id))
      if (downloadDatabase && isGapless) {
        putExtra(QuranDownloadService.EXTRA_DOWNLOAD_DATABASE, fileManager.urlForDatabase(qari))
      }
    }
    appContext.startService(intent)
  }

  override fun downloadAudioDatabase(qari: Qari) {
    val databaseUri = fileManager.urlForDatabase(qari)
    val intent = ServiceIntentHelper.getAudioDownloadIntent(
      appContext,
      databaseUri,
      fileManager.audioFileDirectory() + qari.path,
      appContext.getString(com.quran.mobile.feature.downloadmanager.R.string.audio_manager_database)
    ).apply {
      putExtra(QuranDownloadService.EXTRA_METADATA, AudioDownloadMetadata(qari.id))
    }
    appContext.startService(intent)
  }

  override fun cancelDownloads() {
    val intent = Intent(appContext, QuranDownloadService::class.java).apply {
      action = QuranDownloadService.ACTION_CANCEL_DOWNLOADS
    }
    appContext.startService(intent)
  }

  companion object {
    private const val AUDIO_DOWNLOAD_KEY = "AudioDownload.DownloadKey."
  }
}
