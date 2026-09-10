package com.gantree.cab.drive

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.gantree.cab.CabApp
import com.gantree.cab.R

class CabCarAppService : CarAppService() {
  override fun createHostValidator(): HostValidator {
    // DHU + sideload. Play production would use the Google host allowlist.
    return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
  }

  override fun onCreateSession(sessionInfo: SessionInfo): Session = CabSession()
}

class CabSession : Session() {
  override fun onCreateScreen(intent: Intent): Screen = CabThreadScreen(carContext)
}

class CabThreadScreen(carContext: CarContext) : Screen(carContext) {
  override fun onGetTemplate(): Template {
    val app = carContext.applicationContext as CabApp
    val lines = app.mouth.lines.value.takeLast(6)
    val items = ItemList.Builder()
    if (lines.isEmpty()) {
      items.addItem(
        Row.Builder().setTitle(carContext.getString(R.string.car_empty)).build(),
      )
    } else {
      for (line in lines.asReversed()) {
        val who = if (line.fromYou) "You" else app.prefs.slug
        items.addItem(
          Row.Builder()
            .setTitle(who)
            .addText(line.text)
            .build(),
        )
      }
    }
    return ListTemplate.Builder()
      .setTitle(app.prefs.slug.ifBlank { "cab" })
      .setSingleList(items.build())
      .build()
  }
}
