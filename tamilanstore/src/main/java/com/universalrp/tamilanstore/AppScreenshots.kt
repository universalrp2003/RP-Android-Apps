package com.universalrp.tamilanstore

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/** Real app screens from public galleries or owner-authorized uploads; never synthetic screens. */
@Composable
fun AppScreenshots(appId: String, tamil: Boolean) {
    val context=LocalContext.current
    val files=remember(appId) {
        val root=context.assets.open("screenshots/index.json").bufferedReader().use { JSONObject(it.readText()) }
        val list=root.optJSONArray(appId)
        if(list==null) emptyList() else (0 until list.length()).map { list.getString(it) }
    }
    var selected by remember(appId) { mutableStateOf<String?>(null) }
    if(files.isEmpty()) {
        Text(if(tamil)"திரைப் படங்கள் விரைவில் · உண்மையான படங்கள் இன்னும் சேர்க்கப்படவில்லை." else "Screenshots pending · real app screens not yet supplied.",style=MaterialTheme.typography.labelSmall)
        return
    }
    Text(if(tamil)"முந்தைய செயலித் திரைகள் · பெரிதாக்கத் தொடவும்" else "Earlier app screens · tap to enlarge",style=MaterialTheme.typography.labelSmall)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        files.forEachIndexed { index,name ->
            // Decode to thumbnail dimensions, not full-res for each card.
            val image=remember(name) { context.assets.open("screenshots/$name").use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=4 }) }?.asImageBitmap() }
            if(image!=null) Image(image,contentDescription=if(tamil)"திரைப் படம் ${index+1}; பெரிதாக்கு" else "Screenshot ${index+1}; enlarge",
                modifier=Modifier.width(82.dp).height(146.dp).clickable { selected=name },contentScale=ContentScale.Fit)
        }
    }
    selected?.let { name ->
        val image=remember(name) { context.assets.open("screenshots/$name").use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }
        AlertDialog(onDismissRequest={selected=null},title={Text(if(tamil)"செயலித் திரை" else "App screenshot")},text={
            if(image!=null) Image(image,contentDescription=if(tamil)"பெரிதாக்கிய திரைப் படம்" else "Expanded screenshot",modifier=Modifier.fillMaxWidth().heightIn(max=520.dp),contentScale=ContentScale.Fit)
        },confirmButton={TextButton(onClick={selected=null}){Text(if(tamil)"மூடு" else "Close")}})
    }
}
