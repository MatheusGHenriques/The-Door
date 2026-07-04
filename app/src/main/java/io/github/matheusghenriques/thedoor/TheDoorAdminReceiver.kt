package io.github.matheusghenriques.thedoor

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class TheDoorAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, context.getString(R.string.admin_sealed_toast), Toast.LENGTH_SHORT)
            .show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.admin_disable_warning)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(
            context, context.getString(R.string.admin_unsealed_toast), Toast.LENGTH_SHORT
        ).show()
    }
}