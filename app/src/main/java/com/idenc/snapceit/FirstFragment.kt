package com.idenc.snapceit

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_GALLERY_IMAGE = 2
    private lateinit var currentPhotoPath: Uri

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        view.findViewById<ImageButton>(R.id.cameraButton).setOnClickListener {
            dispatchTakePictureIntent()
        }

        view.findViewById<ImageButton>(R.id.galleryButton).setOnClickListener {
            dispatchGalleryIntent()
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    private fun fixRotation(source: Bitmap, photoPath: Uri): Bitmap {
        val ei = ExifInterface(photoPath.path!!)
        val orientation: Int = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(source, 90F)?.let {return it}
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(source, 180F)?.let {return it}
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(source, 270F)?.let {return it}
        }
        return source
    }

    private fun runTextRecognition(photoPath: Uri) {
        context?.let { ctx ->
            activity?.let {
                var bitmap: Bitmap
                val imageView = it.findViewById<ImageView>(R.id.imageView)
                imageView.visibility = View.VISIBLE
                bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(
                        it.contentResolver,
                        photoPath
                    )
                } else {
                    val source = ImageDecoder.createSource(it.contentResolver, photoPath)
                    ImageDecoder.decodeBitmap(source)
                }
                bitmap = fixRotation(bitmap, photoPath)
                println(bitmap.height)
                println(bitmap.width)
                val image: InputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer: TextRecognizer = TextRecognition.getClient()
                recognizer.process(image)
                    .addOnSuccessListener { texts ->
                        processTextRecognitionResult(bitmap, texts, imageView)
                    }
                    .addOnFailureListener { e -> // Task failed with an exception
                        e.printStackTrace()
                    }
            }
        }
    }

    private fun showToast(text: String) {
        view?.let {
            Snackbar.make(it, text, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun processTextRecognitionResult(resultImg: Bitmap, result: Text, imageView: ImageView) {
        val blocks: List<Text.TextBlock> = result.textBlocks
        if (blocks.isEmpty()) {
            showToast("No text found")
            return
        }
        val paint = Paint()
        paint.setARGB(128, 255, 255, 255)
        //paint.strokeWidth = 1F
        val mutableBitmap = resultImg.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        for (block in blocks) {
            for (line in block.lines) {
                val lineFrame = line.boundingBox
                if (lineFrame != null) {
                    println(line.text)
                    canvas.drawRect(lineFrame, paint)
                }
            }
        }
        imageView.setImageBitmap(mutableBitmap)
    }

    private fun dispatchGalleryIntent() {
        Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.INTERNAL_CONTENT_URI
        ).also { galleryIntent ->
            galleryIntent.resolveActivity(activity!!.packageManager)?.also {
                startActivityForResult(galleryIntent, REQUEST_GALLERY_IMAGE)
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(activity!!.packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    view?.let {
                        Snackbar.make(it, "Failed to create image file.", Snackbar.LENGTH_LONG)
                            .show()
                    }
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also { pFile ->
                    activity?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            it,
                            "com.example.snapceit.fileprovider",
                            pFile
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }
                ?: view?.let {
                    Snackbar.make(it, "Failed to open camera.", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CANADA).format(Date())
        activity?.cacheDir.also { storageDir ->
            return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
            ).apply {
                // Save a file: path for use with ACTION_VIEW intents
                currentPhotoPath = toUri()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY_IMAGE) {
                data?.also {
                    it.data?.also { uri ->
                        runTextRecognition(uri)
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                runTextRecognition(currentPhotoPath)
            }
        }
    }


}