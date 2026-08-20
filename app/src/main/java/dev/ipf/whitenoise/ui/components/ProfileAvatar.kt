package dev.ipf.whitenoise.ui.components

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AvatarAsset
import dev.ipf.whitenoise.model.ProfileAvatar

@Composable
fun ProfileAvatar(
    name: String,
    avatar: ProfileAvatar,
    modifier: Modifier = Modifier,
    contentDescription: String? = if (avatar is ProfileAvatar.Monogram) {
        "Profile photo, ${name.trim().firstOrNull()?.uppercase() ?: "?"}"
    } else {
        "Profile photo"
    },
) {
    val avatarModifier = modifier.clip(CircleShape).then(
        if (contentDescription == null) {
            Modifier.clearAndSetSemantics { }
        } else {
            Modifier.semantics { this.contentDescription = contentDescription }
        },
    )

    when (avatar) {
        is ProfileAvatar.Asset -> {
            Image(
                painter = painterResource(avatar.asset.drawableResource),
                contentDescription = null,
                modifier = avatarModifier,
                contentScale = ContentScale.Crop,
            )
        }

        is ProfileAvatar.WebImage -> {
            Image(
                painter = painterResource(avatar.asset.drawableResource),
                contentDescription = null,
                modifier = avatarModifier,
                contentScale = ContentScale.Crop,
            )
        }

        is ProfileAvatar.DeviceImage -> {
            val bitmap = remember(avatar) {
                BitmapFactory.decodeByteArray(avatar.bytes, 0, avatar.bytes.size)?.asImageBitmap()
            }
            if (bitmap == null) {
                MonogramAvatar(name = name, modifier = avatarModifier)
            } else {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = avatarModifier,
                    contentScale = ContentScale.Crop,
                )
            }
        }

        ProfileAvatar.Monogram -> MonogramAvatar(name = name, modifier = avatarModifier)
    }
}

@Composable
private fun MonogramAvatar(
    name: String,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

val AvatarAsset.drawableResource: Int
    @DrawableRes get() = when (this) {
        AvatarAsset.Badger -> R.drawable.avatar_badger
        AvatarAsset.WebAionyHaust -> R.drawable.avatar_web_aiony_haust
        AvatarAsset.OpenCircuit -> R.drawable.profile_avatar_open_circuit
        AvatarAsset.Fox -> R.drawable.avatar_fox
        AvatarAsset.WebChristopherCampbell -> R.drawable.avatar_web_christopher_campbell
        AvatarAsset.CipherWheel -> R.drawable.profile_avatar_cipher_wheel
        AvatarAsset.Marmot -> R.drawable.avatar_marmot
        AvatarAsset.WebIanDooley -> R.drawable.avatar_web_ian_dooley
        AvatarAsset.Pebble -> R.drawable.profile_avatar_pebble
        AvatarAsset.Ostrich -> R.drawable.avatar_ostrich
        AvatarAsset.WebSergioDePaula -> R.drawable.avatar_web_sergio_de_paula
        AvatarAsset.OpenQuill -> R.drawable.profile_avatar_open_quill
        AvatarAsset.Sloth -> R.drawable.avatar_sloth
        AvatarAsset.WebAyoOgunseinde -> R.drawable.avatar_web_ayo_ogunseinde
        AvatarAsset.FreeSignal -> R.drawable.profile_avatar_free_signal
        AvatarAsset.GardenClub -> R.drawable.avatar_garden_club
        AvatarAsset.WebVinceFleming -> R.drawable.avatar_web_vince_fleming
        AvatarAsset.LibertyRelay -> R.drawable.profile_avatar_liberty_relay
        AvatarAsset.PublicVoice -> R.drawable.profile_avatar_public_voice
        AvatarAsset.WebPhilipMartin -> R.drawable.avatar_web_philip_martin
        AvatarAsset.Marmota -> R.drawable.profile_avatar_marmota
        AvatarAsset.MayaChen -> R.drawable.avatar_maya_chen
        AvatarAsset.EliasMoreno -> R.drawable.avatar_elias_moreno
        AvatarAsset.MinaPark -> R.drawable.avatar_mina_park
        AvatarAsset.LeoMartins -> R.drawable.avatar_leo_martins
        AvatarAsset.NoraBennett -> R.drawable.avatar_nora_bennett
        AvatarAsset.TheoGrant -> R.drawable.avatar_theo_grant
        AvatarAsset.AishaRahman -> R.drawable.avatar_aisha_rahman
        AvatarAsset.LenaOrtiz -> R.drawable.avatar_lena_ortiz
        AvatarAsset.JonahReed -> R.drawable.avatar_jonah_reed
        AvatarAsset.TessaMorgan -> R.drawable.avatar_tessa_morgan
        AvatarAsset.MarcusBell -> R.drawable.avatar_marcus_bell
        AvatarAsset.SofiaAlvarez -> R.drawable.avatar_sofia_alvarez
        AvatarAsset.DanielKim -> R.drawable.avatar_daniel_kim
        AvatarAsset.Fiatjaf -> R.drawable.avatar_fiatjaf
        AvatarAsset.LegacyDavidChaum -> R.drawable.legacy_avatar_david_chaum
        AvatarAsset.LegacyEricHughes -> R.drawable.legacy_avatar_eric_hughes
        AvatarAsset.LegacyHalFinney -> R.drawable.legacy_avatar_hal_finney
        AvatarAsset.LegacyJudithMilhon -> R.drawable.legacy_avatar_judith_milhon
        AvatarAsset.LegacyMarmots -> R.drawable.legacy_avatar_marmots
        AvatarAsset.LegacyNostrDevs -> R.drawable.legacy_avatar_nostr_devs
        AvatarAsset.LegacyRadiaPerlman -> R.drawable.legacy_avatar_radia_perlman
        AvatarAsset.LegacyRichardStallman -> R.drawable.legacy_avatar_richard_stallman
        AvatarAsset.LegacySatoshiNakamoto -> R.drawable.legacy_avatar_satoshi_nakamoto
        AvatarAsset.LegacyWhitfieldDiffie -> R.drawable.legacy_avatar_whitfield_diffie
    }
