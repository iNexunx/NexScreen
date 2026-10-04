package nx.screen.ds.core

import android.content.Context
import android.content.Intent

/**
 * Resultado de una accion sobre otra app, distinguiendo *por que* fallo.
 *
 * Antes `forceStop` devolvia un Boolean y el ViewModel solo podia decir
 * "necesitas backend". Con Shizuku eso era inutil: cuando el binder habia
 * llegado tarde (Shizuku arranca despues de abrir la app, muy habitual) el
 * snapshot `AppShell.active` seguia siendo null, `forceStop` hacia
 * `return false` en silencio y los tres botones parecian no hacer nada.
 */
sealed interface AppActionResult {
    /** La orden llego al sistema. */
    data object Ok : AppActionResult

    /** No hay ningun backend utilizable todavia (Shizuku sin permiso, root no concedido). */
    data object NoBackend : AppActionResult

    /** Habia backend pero la orden fallo; [detail] lleva el stderr del sistema. */
    data class Failed(val detail: String) : AppActionResult
}

object AppActions {

    fun open(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    /**
     * Fuerza el cierre de otra app.
     *
     * Refresca el backend con `force = true` antes de actuar. Sin esto se
     * usaba el snapshot cacheado de `AppShell.active`, que se quedaba obsoleto
     * justo en el caso que mas duele: arrancar Shizuku y usar el boton a
     * continuacion, sin salir y volver a la app para que se reevalua.
     */
    suspend fun forceStop(packageName: String): AppActionResult {
        val shell = resolveShell() ?: return AppActionResult.NoBackend
        val result = shell.run("am force-stop $packageName")
        return when {
            result.isOk -> AppActionResult.Ok
            // Un codigo distinto de 0 con stderr vacio suele ser "el paquete ya
            // no existe"; lo tratamos igual que un fallo para no mentir al usuario.
            result.stderr.isBlank() && result.code == 0 -> AppActionResult.Ok
            else -> AppActionResult.Failed(result.stderr.ifBlank { "exit ${result.code}" })
        }
    }

    /**
     * Devuelve un backend utilizable, reevaluando la deteccion.
     *
     * Se usa `force = true` porque estas acciones las dispara el usuario en ese
     * instante: el resultado cacheado de hace 2s puede ser justo el erroneo.
     */
    private suspend fun resolveShell(): Shell? {
        AppShell.refresh(force = true)
        return AppShell.active
    }
}
