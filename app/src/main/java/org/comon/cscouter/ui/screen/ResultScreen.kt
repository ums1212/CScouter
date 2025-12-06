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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.BoxWithConstraints
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

    // 2. 크롭된 얼굴 이미지 로드
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

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            // 가로 모드 (Row)
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽: 이미지
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (imageBitmap.value != null) {
                        Image(
                            bitmap = imageBitmap.value!!,
                            contentDescription = "Measured Face",
                            modifier = Modifier
                                .size(280.dp) // 가로 모드에서 좀 더 크게
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Spacer(modifier = Modifier.size(280.dp))
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // 오른쪽: 정보 및 버튼
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ResultTitle()
                    Spacer(modifier = Modifier.height(24.dp))
                    PowerValueText(power = power)
                    Spacer(modifier = Modifier.height(32.dp))
                    ResultButtonRow(
                        onRetryClick = onRetry,
                        onShareClick = { shareResult(context) }
                    )
                }
            }
        } else {
            // 세로 모드 (Column)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ResultTitle()

                Spacer(modifier = Modifier.height(32.dp))

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
                    Spacer(modifier = Modifier.size(200.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                PowerValueText(power = power)

                Spacer(modifier = Modifier.height(48.dp))

                ResultButtonRow(
                    onRetryClick = onRetry,
                    onShareClick = { shareResult(context) }
                )
            }
        }
    }
}

// 공유 기능 별도 함수로 분리 (중복 제거)
private fun shareResult(context: android.content.Context) {
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
