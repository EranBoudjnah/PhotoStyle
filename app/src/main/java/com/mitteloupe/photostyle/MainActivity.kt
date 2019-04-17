package com.mitteloupe.photostyle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.mitteloupe.photostyle.glide.GlideApp
import com.mitteloupe.photostyle.glide.transformation.FixedPaletteTransformation
import com.mitteloupe.photostyle.glide.transformation.Palette
import com.mitteloupe.photostyle.glide.transformation.PosterizeTransformation
import com.mitteloupe.photostyle.glide.transformation.ResizeTransformation
import com.mitteloupe.photostyle.graphics.dithering.BayerConverter
import kotlinx.android.synthetic.main.activity_main.preview_image as previewImageView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlideApp
            .with(this)
            .load("https://scontent-ams3-1.xx.fbcdn.net/v/t1.0-9/51677708_10161351768320203_827078278777929728_o.jpg?_nc_cat=109&_nc_ht=scontent-ams3-1.xx&oh=f886f838f290a37769443ab73f01bcc2&oe=5D369F27")
            // "https://www.fresher.ru/manager_content/images2/kadry-veka/big/2-1.jpg"
            // "https://legismusic.com/wp-content/uploads/2017/07/christmas-royalty-free-music.jpg"
            // "https://library.creativecow.net/articles/hodgetts_philip/displacement/DisplacementGradient.gif"
            .transform(
                ResizeTransformation(640),
//                InvertTransformation(this),
//                GreyScaleTransformation(2f),
//                PixelationTransformation(64),
                PosterizeTransformation(this, 7),
//                InvertTransformation(this),
//                OutlineTransformation(this, OutlineTransformation.Mode.OVERLAY),
                ResizeTransformation(160),
                FixedPaletteTransformation(Palette.GameBoy, BayerConverter()),
//                ColorCountTransformation(16),
                ResizeTransformation(640, smoothScale = false),
                CenterInside()
            )
            .into(previewImageView)
    }
}
