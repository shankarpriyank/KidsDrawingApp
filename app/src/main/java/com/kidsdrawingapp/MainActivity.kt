package com.kidsdrawingapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: ImageButton? =
            null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(20.toFloat())


        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(
                        this,
                        R.drawable.pallet_pressed
                )
        )

        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        ib_gallery.setOnClickListener {

            if (isReadStorageAllowed()) {


                val pickPhoto = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(pickPhoto, GALLERY)
            } else {


                requestStoragePermission()
            }
        }

        ib_undo.setOnClickListener {

            drawing_view.onClickUndo()
        }

        ib_save.setOnClickListener {


            if (isReadStorageAllowed()) {

                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            } else {


                requestStoragePermission()
            }
        }
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {


        if (requestCode == STORAGE_PERMISSION_CODE) {


            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                        this@MainActivity,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_LONG
                ).show()
            } else {

                Toast.makeText(
                        this@MainActivity,
                        "Oops you just denied the permission.",
                        Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {


                        iv_background.visibility = View.VISIBLE


                        iv_background.setImageURI(data.data)
                    } else {

                        Toast.makeText(
                                this@MainActivity,
                                "Error in parsing the image or its corrupted.",
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size :")
        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener(View.OnClickListener {
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        })
        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener(View.OnClickListener {
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        })

        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener(View.OnClickListener {
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        })
        brushDialog.show()
    }


    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {

            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()

            drawing_view.setColor(colorTag)

            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(
                            this,
                            R.drawable.pallet_normal
                    )
            )

             mImageButtonCurrentPaint = view
        }
    }


    private fun requestStoragePermission() {


        if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ).toString()
                )
        ) {

        }


        ActivityCompat.requestPermissions(
                this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
                STORAGE_PERMISSION_CODE
        )
    }


    private fun isReadStorageAllowed(): Boolean {


        val result = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
        )


        return result == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Create bitmap from view and returns it
     */
    private fun getBitmapFromView(view: View): Bitmap {


        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)

        val bgDrawable = view.background
        if (bgDrawable != null) {

            bgDrawable.draw(canvas)
        } else {

            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }


    @SuppressLint("StaticFieldLeak")
    private inner class BitmapAsyncTask(val mBitmap: Bitmap?) :
            AsyncTask<Any, Void, String>() {

        /**
         * ProgressDialog is a modal dialog, which prevents the user from interacting with the app.
         *
         * The progress dialog in newer versions is deprecated so we will create a custom progress dialog later on.
         * This is just an idea to use progress dialog.
         */

        @Suppress("DEPRECATION")
        private var mDialog: ProgressDialog? = null

        override fun onPreExecute() {
            super.onPreExecute()

            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any): String {

            var result = ""

            if (mBitmap != null) {

                try {
                    val bytes = ByteArrayOutputStream()

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)


                    val f = File(
                            externalCacheDir!!.absoluteFile.toString()
                                    + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    )

                    val fo =
                            FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            cancelProgressDialog()

            if (!result.isEmpty()) {
                Toast.makeText(
                        this@MainActivity,
                        "File saved successfully :$result",
                        Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                        this@MainActivity,
                        "Something went wrong while saving the file.",
                        Toast.LENGTH_SHORT
                ).show()
            }






            MediaScannerConnection.scanFile(
                    this@MainActivity, arrayOf(result), null
            ) { path, uri ->
                // This is used for sharing the image after it has being stored in the storage.
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.putExtra(
                            Intent.EXTRA_STREAM,
                            uri
                    ) // A content: URI holding a stream of data associated with the Intent, used to supply the data being sent.
                    shareIntent.type =
                            "image/jpeg" // The MIME type of the data being handled by this intent.
                    startActivity(
                            Intent.createChooser(
                                    shareIntent,
                                    "Share"
                            )
                    )
            }

        }


        private fun showProgressDialog() {
            @Suppress("DEPRECATION")
            mDialog = ProgressDialog.show(
                    this@MainActivity,
                    "",
                    "Saving your image..."
            )
        }


        private fun cancelProgressDialog() {
            if (mDialog != null) {
                mDialog!!.dismiss()
                mDialog = null
            }
        }
    }

    companion object {


        private const val STORAGE_PERMISSION_CODE = 1


        private const val GALLERY = 2
    }
}