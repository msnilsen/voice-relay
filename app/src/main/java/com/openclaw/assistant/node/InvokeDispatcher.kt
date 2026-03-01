package com.openclaw.assistant.node

import com.openclaw.assistant.gateway.GatewaySession
import com.openclaw.assistant.protocol.OpenClawCanvasA2UICommand
import com.openclaw.assistant.protocol.OpenClawCanvasCommand
import com.openclaw.assistant.protocol.OpenClawCameraCommand
import com.openclaw.assistant.protocol.OpenClawDeviceCommand
import com.openclaw.assistant.protocol.OpenClawLocationCommand
import com.openclaw.assistant.protocol.OpenClawScreenCommand
import com.openclaw.assistant.protocol.OpenClawSmsCommand
import com.openclaw.assistant.protocol.OpenClawNotificationsCommand
import com.openclaw.assistant.protocol.OpenClawSystemCommand
import com.openclaw.assistant.protocol.OpenClawPhotosCommand
import com.openclaw.assistant.protocol.OpenClawContactsCommand
import com.openclaw.assistant.protocol.OpenClawCalendarCommand
import com.openclaw.assistant.protocol.OpenClawMotionCommand

class InvokeDispatcher(
  private val canvas: CanvasController,
  private val cameraHandler: CameraHandler,
  private val locationHandler: LocationHandler,
  private val screenHandler: ScreenHandler,
  private val smsHandler: SmsHandler,
  private val notificationsHandler: NotificationsHandler,
  private val systemHandler: SystemHandler,
  private val photosHandler: PhotosHandler,
  private val contactsHandler: ContactsHandler,
  private val calendarHandler: CalendarHandler,
  private val motionHandler: MotionHandler,
  private val a2uiHandler: A2UIHandler,
  private val debugHandler: DebugHandler,
  private val appUpdateHandler: AppUpdateHandler,
  private val deviceHandler: DeviceHandler,
  private val isForeground: () -> Boolean,
  private val cameraEnabled: () -> Boolean,
  private val locationEnabled: () -> Boolean,
) {
  suspend fun handleInvoke(command: String, paramsJson: String?): GatewaySession.InvokeResult {
    // Check foreground requirement for canvas/camera/screen commands
    if (
      command.startsWith(OpenClawCanvasCommand.NamespacePrefix) ||
        command.startsWith(OpenClawCanvasA2UICommand.NamespacePrefix) ||
        command.startsWith(OpenClawCameraCommand.NamespacePrefix) ||
        command.startsWith(OpenClawScreenCommand.NamespacePrefix)
    ) {
      if (!isForeground()) {
        return GatewaySession.InvokeResult.error(
          code = "NODE_BACKGROUND_UNAVAILABLE",
          message = "NODE_BACKGROUND_UNAVAILABLE: canvas/camera/screen commands require foreground",
        )
      }
    }

    // Check camera enabled
    if (command.startsWith(OpenClawCameraCommand.NamespacePrefix) && !cameraEnabled()) {
      return GatewaySession.InvokeResult.error(
        code = "CAMERA_DISABLED",
        message = "CAMERA_DISABLED: enable Camera in Settings",
      )
    }

    // Check location enabled
    if (command.startsWith(OpenClawLocationCommand.NamespacePrefix) && !locationEnabled()) {
      return GatewaySession.InvokeResult.error(
        code = "LOCATION_DISABLED",
        message = "LOCATION_DISABLED: enable Location in Settings",
      )
    }

    return when (command) {
      // Canvas commands
      OpenClawCanvasCommand.Present.rawValue -> {
        val url = CanvasController.parseNavigateUrl(paramsJson)
        canvas.navigate(url)
        GatewaySession.InvokeResult.ok(null)
      }
      OpenClawCanvasCommand.Hide.rawValue -> GatewaySession.InvokeResult.ok(null)
      OpenClawCanvasCommand.Navigate.rawValue -> {
        val url = CanvasController.parseNavigateUrl(paramsJson)
        canvas.navigate(url)
        GatewaySession.InvokeResult.ok(null)
      }
      OpenClawCanvasCommand.Eval.rawValue -> {
        val js =
          CanvasController.parseEvalJs(paramsJson)
            ?: return GatewaySession.InvokeResult.error(
              code = "INVALID_REQUEST",
              message = "INVALID_REQUEST: javaScript required",
            )
        val result =
          try {
            canvas.eval(js)
          } catch (err: Throwable) {
            return GatewaySession.InvokeResult.error(
              code = "NODE_BACKGROUND_UNAVAILABLE",
              message = "NODE_BACKGROUND_UNAVAILABLE: canvas unavailable",
            )
          }
        GatewaySession.InvokeResult.ok("""{"result":${result.toJsonString()}}""")
      }
      OpenClawCanvasCommand.Snapshot.rawValue -> {
        val snapshotParams = CanvasController.parseSnapshotParams(paramsJson)
        val base64 =
          try {
            canvas.snapshotBase64(
              format = snapshotParams.format,
              quality = snapshotParams.quality,
              maxWidth = snapshotParams.maxWidth,
            )
          } catch (err: Throwable) {
            return GatewaySession.InvokeResult.error(
              code = "NODE_BACKGROUND_UNAVAILABLE",
              message = "NODE_BACKGROUND_UNAVAILABLE: canvas unavailable",
            )
          }
        GatewaySession.InvokeResult.ok("""{"format":"${snapshotParams.format.rawValue}","base64":"$base64"}""")
      }

      // A2UI commands
      OpenClawCanvasA2UICommand.Reset.rawValue -> {
        val a2uiUrl = a2uiHandler.resolveA2uiHostUrl()
          ?: return GatewaySession.InvokeResult.error(
            code = "A2UI_HOST_NOT_CONFIGURED",
            message = "A2UI_HOST_NOT_CONFIGURED: gateway did not advertise canvas host",
          )
        val ready = a2uiHandler.ensureA2uiReady(a2uiUrl)
        if (!ready) {
          return GatewaySession.InvokeResult.error(
            code = "A2UI_HOST_UNAVAILABLE",
            message = "A2UI host not reachable",
          )
        }
        val res = canvas.eval(A2UIHandler.a2uiResetJS)
        GatewaySession.InvokeResult.ok(res)
      }
      OpenClawCanvasA2UICommand.Push.rawValue, OpenClawCanvasA2UICommand.PushJSONL.rawValue -> {
        val messages =
          try {
            a2uiHandler.decodeA2uiMessages(command, paramsJson)
          } catch (err: Throwable) {
            return GatewaySession.InvokeResult.error(
              code = "INVALID_REQUEST",
              message = err.message ?: "invalid A2UI payload"
            )
          }
        val a2uiUrl = a2uiHandler.resolveA2uiHostUrl()
          ?: return GatewaySession.InvokeResult.error(
            code = "A2UI_HOST_NOT_CONFIGURED",
            message = "A2UI_HOST_NOT_CONFIGURED: gateway did not advertise canvas host",
          )
        val ready = a2uiHandler.ensureA2uiReady(a2uiUrl)
        if (!ready) {
          return GatewaySession.InvokeResult.error(
            code = "A2UI_HOST_UNAVAILABLE",
            message = "A2UI host not reachable",
          )
        }
        val js = A2UIHandler.a2uiApplyMessagesJS(messages)
        val res = canvas.eval(js)
        GatewaySession.InvokeResult.ok(res)
      }

      // Camera commands
      OpenClawCameraCommand.Snap.rawValue -> cameraHandler.handleSnap(paramsJson)
      OpenClawCameraCommand.Clip.rawValue -> cameraHandler.handleClip(paramsJson)
      OpenClawCameraCommand.List.rawValue -> cameraHandler.handleList(paramsJson)

      // Location command
      OpenClawLocationCommand.Get.rawValue -> locationHandler.handleLocationGet(paramsJson)

      // Screen command
      OpenClawScreenCommand.Record.rawValue -> screenHandler.handleScreenRecord(paramsJson)

      // SMS command
      OpenClawSmsCommand.Send.rawValue -> smsHandler.handleSmsSend(paramsJson)

      // Notifications commands
      OpenClawNotificationsCommand.List.rawValue -> notificationsHandler.handleList()
      OpenClawNotificationsCommand.Actions.rawValue -> notificationsHandler.handleActions(paramsJson)

      // System command
      OpenClawSystemCommand.Notify.rawValue -> systemHandler.handleNotify(paramsJson)

      // Photos command
      OpenClawPhotosCommand.Latest.rawValue -> photosHandler.handleLatest()

      // Contacts commands
      OpenClawContactsCommand.Search.rawValue -> contactsHandler.handleSearch(paramsJson)
      OpenClawContactsCommand.Add.rawValue -> contactsHandler.handleAdd(paramsJson)

      // Calendar commands
      OpenClawCalendarCommand.Events.rawValue -> calendarHandler.handleEvents(paramsJson)
      OpenClawCalendarCommand.Add.rawValue -> calendarHandler.handleAdd(paramsJson)

      // Motion commands
      OpenClawMotionCommand.Activity.rawValue -> motionHandler.handleActivity()
      OpenClawMotionCommand.Pedometer.rawValue -> motionHandler.handlePedometer()

      // Device commands
      OpenClawDeviceCommand.Status.rawValue -> deviceHandler.handleStatus()
      OpenClawDeviceCommand.Info.rawValue -> deviceHandler.handleInfo()
      OpenClawDeviceCommand.Permissions.rawValue -> deviceHandler.handlePermissions()
      OpenClawDeviceCommand.Health.rawValue -> deviceHandler.handleHealth()

      // Debug commands
      "debug.ed25519" -> debugHandler.handleEd25519()
      "debug.logs" -> debugHandler.handleLogs()

      // App update
      "app.update" -> appUpdateHandler.handleUpdate(paramsJson)

      else ->
        GatewaySession.InvokeResult.error(
          code = "INVALID_REQUEST",
          message = "INVALID_REQUEST: unknown command",
        )
    }
  }
}
