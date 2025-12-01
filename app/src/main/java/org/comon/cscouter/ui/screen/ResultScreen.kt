package org.comon.cscouter.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.comon.cscouter.ui.component.PowerValueText
import org.comon.cscouter.ui.component.ResultButtonRow
import org.comon.cscouter.ui.component.ResultTitle
import java.io.File
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap

@Composable
fun ResultScreen(
    power: Int,
    imageUri: String,
    onRetry: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. 타이틀
        ResultTitle()

        Spacer(modifier = Modifier.height(32.dp))

        // 2. 크롭된 얼굴 이미지
        val decodedUri = Uri.decode(imageUri)
        val imageBitmap = produceState<ImageBitmap?>(initialValue = null, key1 = decodedUri) {
            value = withContext(Dispatchers.IO) {
                try {
                    val uri = decodedUri.toUri()
                    val path = uri.path
                    if (path != null) {
                        val file = File(path)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        if (imageBitmap.value != null) {
            Image(
                bitmap = imageBitmap.value!!,
                contentDescription = "Measured Face",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            // 로딩 중이거나 실패 시 대체 UI (여기서는 빈 공간)
            Spacer(modifier = Modifier.size(200.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. 전투력 텍스트
        PowerValueText(power = power)

        Spacer(modifier = Modifier.height(48.dp))

        // 4. 버튼 Row
        ResultButtonRow(
            onRetryClick = onRetry,
            onShareClick = {
                val view = (context as? android.app.Activity)?.window?.decorView?.rootView
                if (view != null) {
                    val bitmap = createBitmap(view.width, view.height)
                    val canvas = android.graphics.Canvas(bitmap)
                    view.draw(canvas)

                    try {
                        val file = File(context.cacheDir, "cscouter_result_${System.currentTimeMillis()}.png")
                        java.io.FileOutputStream(file).use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                        }

                        val contentUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "전투력 공유하기"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
}
