package tech.yangle.matriximage
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import tech.yangle.matriximage.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import android.content.ContentResolver
import androidx.core.content.FileProvider


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var bitmap:Bitmap

    companion object {
        const val ALBUM_RESULT_CODE = 0x999
        const val REQUEST_CODE_PERMISSION_STORAGE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //这一两行代码主要是向用户请求权限
        // 检查是否具有写外部存储的权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
        binding.submitImage.setOnClickListener{openSysAlbum()}


        binding.mivSample.setOnImageLongClickListener { view, pointF ->
            Toast.makeText(this, "mivSample1 长按事件 x:${pointF.x} y:${pointF.y}", Toast.LENGTH_LONG).show()
        }



    }

     //onRequestPermissionsResult 回调处理权限请求的结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 处理权限请求结果
    }


    // onActivityResult 回调处理从其他 Activity 返回的结果
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //设置了一个条件语句来检查请求码是否与预期的相册请求码 ALBUM_RESULT_CODE 匹配，结果码是否表示操作成功，以及返回的 Intent 数据是否不为空。
        if (requestCode == ALBUM_RESULT_CODE && resultCode == Activity.RESULT_OK && data != null) {
            //调用 handleImageOnKitKat 函数来处理返回的图片 Intent。
            handleImageOnKitKat(data)
        }
    }

    // openSysAlbum 函数打开系统相册供用户选择图片
    @SuppressLint("IntentReset")
    private fun openSysAlbum() {
        val albumIntent = Intent(Intent.ACTION_PICK).apply {
            // 设置 intent 的 type 为 "image/*"，这表明我们只对图片文件感兴趣
            type = "image/*"
            // 设置 intent 的 data 为 MediaStore.Images.Media.EXTERNAL_CONTENT_URI，这指向设备上的外部图片存储
            data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        // // 使用 startActivityForResult 方法启动相册选择，
        // 并通过传入的 Intent 和请求码 ALBUM_RESULT_CODE 来处理返回结果
        startActivityForResult(albumIntent, ALBUM_RESULT_CODE)
    }


    /**
     * 这个函数主要处理两种类型的 URI：
     *
     * Document 类型的 URI：
     * 这是 Android 4.4（KitKat）引入的一种新的 URI 形式，
     * 用于访问存储在不同存储提供商中的文件。
     * 函数通过 DocumentsContract 来解析这些 URI，
     * 并根据不同的 authority 来获取实际的图片路径。
     *
     * Content 和 File 类型的 URI：
     * 对于 content 类型的 URI，函数直接查询 MediaStore 来获取图片路径；
     * 对于 file 类型的 URI，函数直接使用 URI 的 path 作为文件路径。
     */
    // handleImageOnKitKat 函数处理从系统相册返回的图片 URI
    private fun handleImageOnKitKat(data: Intent?) {
        var imagePath: String? = null
        // 从传递给该函数的 Intent 中获取数据（即选中图片的 URI）
        val uri = data?.data
        // 检查 uri 是否不为空，并且是 Document 类型的 URI
        if (uri != null && DocumentsContract.isDocumentUri(this, uri)) {
            // 获取 Document URI 的文档 ID
            val docId = DocumentsContract.getDocumentId(uri)
            // 获取 URI 的 authority
            val authority = uri.authority
            when (authority) {
                "com.android.providers.media.documents" -> {
                    // 解析文档 ID 来获取图片在 MediaStore 中的 ID
                    val id = docId.split(":")[1]
                    // 构建查询 MediaStore 的条件
                    val selection = "${MediaStore.Images.Media._ID} = $id"
                    // 调用 getImagePath 函数来获取图片的实际路径
                    imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection)
                }
                "com.android.providers.downloads.documents" -> {
                    // 解析文档 ID 并获取下载内容的 URI
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content: //downloads/public_downloads"),
                        //这行代码接收一个 String 类型的 docId，尝试将其解析为一个长整型（long）的数值，并返回该数值对应的 Long 对象。
                        //用于将字符串转换为 Long 类型的对象
                        java.lang.Long.valueOf(docId)
                    )
                    // 调用 getImagePath 函数来获取图片的实际路径
                    imagePath = getImagePath(contentUri, null)
                }
            }
        } else {
            // 如果 uri 不是 Document 类型，则根据 scheme 来获取图片路径
            if ("content" == uri?.scheme) {
                imagePath = getImagePath(uri, null)
            // 对于 file 类型的 URI，直接获取路径
            } else if ("file" == uri?.scheme)
            {
                imagePath = uri.path
            }
        }
        // 如果 imagePath 不为空，则调用 displayImage 函数来显示图片
        imagePath?.let { displayImage(it) }
    }

    @SuppressLint("Range")
    private fun getImagePath(uri: Uri, selection: String?): String? {
        // 使用 contentResolver 执行查询操作，查询与给定 URI 相关的数据
        val cursor = contentResolver.query(uri, null, selection, null, null)
        var path: String? = null
        // 使用 cursor?.use 来确保游标在操作完成后会被关闭，即使发生异常也是如此
        cursor?.use {
            // 将游标移动到第一行数据
            if (it.moveToFirst()) {
                // 获取图片路径的列索引，使用 MediaStore.Images.Media.DATA 作为列名
                // 从游标中取出第一行的图片路径数据，并将其赋值给 path 变量
                path = it.getString(it.getColumnIndex(MediaStore.Images.Media.DATA))
            }
        }
        return path
    }

    private fun displayImage(imagePath: String) {
        // 使用 BitmapFactory.decodeFile 将文件路径 imagePath 对应的图片文件解码成 Bitmap 对象
        // 并将这个 Bitmap 对象赋值给已声明的 lateinit 变量 bitmap
        bitmap = BitmapFactory.decodeFile(imagePath)
        // 通过 DataBinding 的 binding 对象访问布局文件中定义的 mivSample ImageView
        // 并调用 ImageView 的 setBitmap 方法，将 bitmap 设置到 ImageView 上，从而显示图片
        binding.mivSample.setImageBitmap(bitmap)
    }
}