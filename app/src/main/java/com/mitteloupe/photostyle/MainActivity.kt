package com.mitteloupe.photostyle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.mitteloupe.photostyle.glide.GlideApp
import com.mitteloupe.photostyle.glide.transformation.GreyScaleTransformation
import com.mitteloupe.photostyle.glide.transformation.InvertTransformation
import com.mitteloupe.photostyle.glide.transformation.PixelationTransformation
import com.mitteloupe.photostyle.glide.transformation.PosterizeTransformation
import kotlinx.android.synthetic.main.activity_main.preview_image as previewImageView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlideApp
            .with(this)
            .load("https://homepages.cae.wisc.edu/~ece533/images/tulips.png")
            .transform(
                InvertTransformation(this),
                GreyScaleTransformation(2f),
                PixelationTransformation(160),
                PosterizeTransformation(this, 3),
                InvertTransformation(this),
                CenterInside()
            )
            .into(previewImageView)
    }
}
