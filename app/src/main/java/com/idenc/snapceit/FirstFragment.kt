package com.idenc.snapceit

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_GALLERY_IMAGE = 2
    private lateinit var currentPhotoPath: Uri
    private lateinit var fragmentAdapter: RecyclerAdapter
    private var itemsList = ArrayList<Pair<String, String>>()

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
        fragmentAdapter = RecyclerAdapter(itemsList)

        view.findViewById<RecyclerView>(R.id.recycler).apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context!!)
            this.adapter = fragmentAdapter
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

    private fun fixRotation(source: Bitmap, stream: InputStream): Bitmap {
        val ei = ExifInterface(stream)
        val orientation: Int = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(source, 90F)?.let { return it }
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(source, 180F)?.let { return it }
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(source, 270F)?.let { return it }
        }
        return source
    }

    private fun runTextRecognition(photoPath: Uri) {
        context?.let { ctx ->
            activity?.let {
                var bitmap: Bitmap
                val recyclerView = it.findViewById<RecyclerView>(R.id.recycler)
                recyclerView.visibility = View.VISIBLE
                bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(
                        it.contentResolver,
                        photoPath
                    )
                } else {
                    val source = ImageDecoder.createSource(it.contentResolver, photoPath)
                    ImageDecoder.decodeBitmap(source)
                }
                it.contentResolver.openInputStream(photoPath)?.let { stream ->
                    bitmap = fixRotation(bitmap, stream)
                }

                val image: InputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer: TextRecognizer = TextRecognition.getClient()
                recognizer.process(image)
                    .addOnSuccessListener { texts ->
                        processTextRecognitionResult(texts, bitmap.height)
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

    fun closest(of: Text.Line, blocks: List<Text.TextBlock>): Pair<Text.Line, Int> {
        var min = Int.MAX_VALUE
        var closest = of
        var diff = -1
        for (block in blocks) {
            val lines = block.lines
            for (line in lines) {
                if (line.text.contains("$") && line.text != of.text) {
                    diff = abs(line.boundingBox!!.bottom - of.boundingBox!!.bottom)
                    if (diff < min) {
                        min = diff
                        closest = line
                    }
                }
            }
        }
        return Pair(closest, diff)
    }

    private fun processTextRecognitionResult(result: Text, height: Int) {
        val blocks: List<Text.TextBlock> = result.textBlocks
        if (blocks.isEmpty()) {
            showToast("No text found")
            return
        }

        itemsList.clear()

        for (block in blocks) {
            val lines = block.lines
            // For each detected line
            for (line in lines) {
                // If there is no $ hopefully we have an item
                if (!line.text.contains("$")) {
                    val (closestPrice, diff) = closest(line, blocks)

                    if (((diff.toDouble() / height) * 100) < 50) {
                        itemsList.add(Pair(line.text, closestPrice.text))
                    }
                    println("${line.text} \t ${closestPrice.text}")
                    println((diff.toDouble() / height) * 100)
                    println()
                }
            }
        }
        fragmentAdapter.notifyDataSetChanged()
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