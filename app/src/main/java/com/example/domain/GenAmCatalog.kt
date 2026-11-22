package com.example.domain

/**
 * Curated General American English (GenAm) articulatory catalog and progressive shadowing deck.
 */
object GenAmCatalog {

    val restPose = ArticulationPose(
        jawDrop = 0.18f,
        lipRounding = 0.10f,
        lipAperture = 0.20f,
        tongueTipX = 0.30f,
        tongueTipY = 0.62f,
        tongueBladeX = 0.42f,
        tongueBladeY = 0.57f,
        tongueDorsumX = 0.58f,
        tongueDorsumY = 0.53f,
        tongueRootX = 0.72f,
        tongueRootY = 0.64f,
        velumOpen = 0.0f,
        voiced = false,
        airflowType = AirflowType.LAMINAR_OPEN,
        contactLabel = "Neutral Rest Position"
    )

    // Ghost contrast helper poses (for /s/, /l/, /ɛ/ contrasts not in main 12)
    val contrastExtraPoses: Map<String, Pair<String, ArticulationPose>> = mapOf(
        "s_alveolar" to (
            "/s/ Alveolar S" to ArticulationPose(
                jawDrop = 0.16f,
                lipRounding = 0.05f,
                lipAperture = 0.22f,
                tongueTipX = 0.28f,
                tongueTipY = 0.38f,
                tongueBladeX = 0.38f,
                tongueBladeY = 0.41f,
                tongueDorsumX = 0.56f,
                tongueDorsumY = 0.48f,
                tongueRootX = 0.71f,
                tongueRootY = 0.62f,
                velumOpen = 0.0f,
                voiced = false,
                airflowType = AirflowType.CONTINUOUS_ORAL,
                contactLabel = "Grooved near alveolar ridge (behind teeth)"
            )
        ),
        "l_clear" to (
            "/l/ Clear L" to ArticulationPose(
                jawDrop = 0.30f,
                lipRounding = 0.08f,
                lipAperture = 0.36f,
                tongueTipX = 0.27f,
                tongueTipY = 0.35f,
                tongueBladeX = 0.39f,
                tongueBladeY = 0.48f,
                tongueDorsumX = 0.56f,
                tongueDorsumY = 0.55f,
                tongueRootX = 0.72f,
                tongueRootY = 0.65f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.LAMINAR_OPEN,
                contactLabel = "Tip touches alveolar ridge; sides open"
            )
        ),
        "eh_vowel" to (
            "/ɛ/ Dress Vowel" to ArticulationPose(
                jawDrop = 0.46f,
                lipRounding = 0.05f,
                lipAperture = 0.48f,
                tongueTipX = 0.28f,
                tongueTipY = 0.60f,
                tongueBladeX = 0.40f,
                tongueBladeY = 0.52f,
                tongueDorsumX = 0.57f,
                tongueDorsumY = 0.53f,
                tongueRootX = 0.71f,
                tongueRootY = 0.65f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.LAMINAR_OPEN,
                contactLabel = "Mid-front relaxed jaw (less open than /æ/)"
            )
        )
    )

    val phonemes: List<GenAmPhoneme> = listOf(
        GenAmPhoneme(
            id = "th_voiceless",
            ipa = "/θ/",
            name = "Voiceless Dental TH",
            category = PhonemeCategory.DENTAL,
            exampleWord = "think",
            exampleIpa = "/θɪŋk/",
            placementCue = "Slide the very tip of your tongue gently between your upper and lower front teeth without biting down.",
            airflowCue = "Push a steady, quiet stream of unvoiced air over the tongue tip. Throat stays completely still.",
            commonMistake = "Pulling the tongue behind the teeth produces /s/ ('sink') or pressing firmly produces /t/ ('tink').",
            contrastPhonemeId = "s_alveolar",
            contrastLabel = "Compare /θ/ vs /s/",
            pose = ArticulationPose(
                jawDrop = 0.24f,
                lipRounding = 0.05f,
                lipAperture = 0.30f,
                tongueTipX = 0.16f,
                tongueTipY = 0.45f,
                tongueBladeX = 0.32f,
                tongueBladeY = 0.48f,
                tongueDorsumX = 0.54f,
                tongueDorsumY = 0.50f,
                tongueRootX = 0.71f,
                tongueRootY = 0.63f,
                velumOpen = 0.0f,
                voiced = false,
                airflowType = AirflowType.CONTINUOUS_ORAL,
                contactLabel = "Tongue tip lightly touches upper incisors"
            )
        ),
        GenAmPhoneme(
            id = "th_voiced",
            ipa = "/ð/",
            name = "Voiced Dental TH",
            category = PhonemeCategory.DENTAL,
            exampleWord = "weather",
            exampleIpa = "/ˈwɛð.ɚ/",
            placementCue = "Identical interdental tongue placement to /θ/, resting softly against the edge of the upper front teeth.",
            airflowCue = "Engage your vocal folds immediately so you feel a warm buzz on the tongue tip.",
            commonMistake = "Stopping airflow completely turns 'they' into 'day' (/d/); retracting turns it into /z/.",
            contrastPhonemeId = "th_voiceless",
            contrastLabel = "Compare /ð/ vs /θ/",
            pose = ArticulationPose(
                jawDrop = 0.26f,
                lipRounding = 0.06f,
                lipAperture = 0.32f,
                tongueTipX = 0.17f,
                tongueTipY = 0.44f,
                tongueBladeX = 0.33f,
                tongueBladeY = 0.47f,
                tongueDorsumX = 0.54f,
                tongueDorsumY = 0.49f,
                tongueRootX = 0.70f,
                tongueRootY = 0.62f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.CONTINUOUS_ORAL,
                contactLabel = "Interdental contact + vocal fold vibration"
            )
        ),
        GenAmPhoneme(
            id = "r_american",
            ipa = "/ɹ/",
            name = "American Rhotic R",
            category = PhonemeCategory.RHOTIC,
            exampleWord = "really",
            exampleIpa = "/ˈɹɪ.li/",
            placementCue = "Pull the tongue body slightly back and bunch the mid-tongue upward toward the palate. Keep the tip floating—never touch the roof.",
            airflowCue = "Round your lips gently forward while humming a continuous voiced resonance.",
            commonMistake = "Tapping the alveolar ridge makes a Spanish flap or /l/; flattening the lips weakens American rhoticity.",
            contrastPhonemeId = "l_clear",
            contrastLabel = "Compare /ɹ/ vs /l/",
            pose = ArticulationPose(
                jawDrop = 0.30f,
                lipRounding = 0.72f,
                lipAperture = 0.34f,
                tongueTipX = 0.39f,
                tongueTipY = 0.47f,
                tongueBladeX = 0.48f,
                tongueBladeY = 0.38f,
                tongueDorsumX = 0.61f,
                tongueDorsumY = 0.43f,
                tongueRootX = 0.74f,
                tongueRootY = 0.58f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.LAMINAR_OPEN,
                contactLabel = "Bunched mid-tongue; tip floats free"
            )
        ),
        GenAmPhoneme(
            id = "er_rhotic",
            ipa = "/ɝ/",
            name = "Stressed R-Colored Vowel",
            category = PhonemeCategory.RHOTIC,
            exampleWord = "world",
            exampleIpa = "/wɝld/",
            placementCue = "Hold the bunched American /ɹ/ tongue tension right in the center of the mouth while bracing the tongue sides against the upper molars.",
            airflowCue = "Sustained voiced resonant vowel with soft lip rounding.",
            commonMistake = "Dropping the tongue curl turns 'bird' or 'work' into a flat vowel without GenAm r-coloring.",
            contrastPhonemeId = "r_american",
            contrastLabel = "Compare /ɝ/ vs /ɹ/",
            pose = ArticulationPose(
                jawDrop = 0.34f,
                lipRounding = 0.64f,
                lipAperture = 0.38f,
                tongueTipX = 0.42f,
                tongueTipY = 0.50f,
                tongueBladeX = 0.51f,
                tongueBladeY = 0.40f,
                tongueDorsumX = 0.62f,
                tongueDorsumY = 0.44f,
                tongueRootX = 0.74f,
                tongueRootY = 0.59f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.LAMINAR_OPEN,
                contactLabel = "Central bunched rhotic vowel"
            )
        ),
        GenAmPhoneme(
            id = "ae_vowel",
            ipa = "/æ/",
            name = "Open Front Ash Vowel",
            category = PhonemeCategory.VOWEL,
            exampleWord = "matter",
            exampleIpa = "/ˈmæ.ɾɚ/",
            placementCue = "Drop your jaw noticeably wider than /ɛ/ while keeping the tongue tip anchored behind your lower front teeth and lips slightly spread.",
            airflowCue = "Bright, open voiced resonance straight out of the front oral cavity.",
            commonMistake = "Not dropping the jaw enough merges 'bad' into 'bed' (/ɛ/) or 'hat' into 'hot' (/ɑ/).",
            contrastPhonemeId = "eh_vowel",
            contrastLabel = "Compare /æ/ vs /ɛ/",
            pose = ArticulationPose(
                jawDrop = 0.82f,
                lipRounding = 0.0f,
                lipAperture = 0.78f,
                tongueTipX = 0.25f,
                tongueTipY = 0.70f,
                tongueBladeX = 0.39f,
                tongueBladeY = 0.62f,
                tongueDorsumX = 0.57f,
                tongueDorsumY = 0.60f,
                tongueRootX = 0.71f,
                tongueRootY = 0.68f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.LAMINAR_OPEN,
                contactLabel = "Wide jaw drop; low-front tongue"
            )
        ),
        GenAmPhoneme(
            id = "ih_vowel",
            ipa = "/ɪ/",
            name = "Lax Short I",
            category = PhonemeCategory.VOWEL,
            exampleWord = "ship",
            exampleIpa = "/ʃɪp/",
            placementCue = "Relax your cheeks and tongue slightly lower and further back than /iː/. Avoid smiling tensely.",
            airflowCue = "Short, relaxed voiced vowel pulse.",
            commonMistake = "Over-tensing the lips and raising the tongue turns 'live' into 'leave' (/iː/).",
            contrastPhonemeId = "iy_vowel",
            contrastLabel = "Compare /ɪ/ vs /iː/",
            pose = ArticulationPose(
                jawDrop = 0.32f,
                lipRounding = 0.05f,
                lipAperture = 0.36f,
                tongueTipX = 0.28f,
                tongueTipY = 0.56f,
                tongueBladeX = 0.40f,
                tongueBladeY = 0.45f,
                tongueDorsumX = 0.56f,
                tongueDorsumY = 0.48f,
                tongueRootX = 0.70f,
                tongueRootY = 0.62f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.LAMINAR_OPEN,
                contactLabel = "High-front relaxed tongue & soft lips"
            )
        ),
        GenAmPhoneme(
            id = "iy_vowel",
            ipa = "/iː/",
            name = "Tense Long EE",
            category = PhonemeCategory.VOWEL,
            exampleWord = "sheep",
            exampleIpa = "/ʃiːp/",
            placementCue = "Arch the front blade of the tongue high toward the hard palate with gentle horizontal lip spread.",
            airflowCue = "Clear, sustained voiced high-front resonance.",
            commonMistake = "Cutting the vowel too short blurs the contrast between 'seat' and 'sit'.",
            contrastPhonemeId = "ih_vowel",
            contrastLabel = "Compare /iː/ vs /ɪ/",
            pose = ArticulationPose(
                jawDrop = 0.18f,
                lipRounding = 0.0f,
                lipAperture = 0.24f,
                tongueTipX = 0.26f,
                tongueTipY = 0.50f,
                tongueBladeX = 0.37f,
                tongueBladeY = 0.36f,
                tongueDorsumX = 0.54f,
                tongueDorsumY = 0.41f,
                tongueRootX = 0.69f,
                tongueRootY = 0.58f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.LAMINAR_OPEN,
                contactLabel = "High tense front arch + spread lips"
            )
        ),
        GenAmPhoneme(
            id = "uh_vowel",
            ipa = "/ʊ/",
            name = "Lax Book Vowel",
            category = PhonemeCategory.VOWEL,
            exampleWord = "could",
            exampleIpa = "/kʊd/",
            placementCue = "Raise the back of the tongue moderately with only soft, loose lip rounding—not a tight whistle.",
            airflowCue = "Brief, relaxed back-cavity voiced vowel.",
            commonMistake = "Pursing lips tightly turns 'full' into 'fool' (/uː/) or 'pull' into 'pool'.",
            contrastPhonemeId = "uw_vowel",
            contrastLabel = "Compare /ʊ/ vs /uː/",
            pose = ArticulationPose(
                jawDrop = 0.34f,
                lipRounding = 0.48f,
                lipAperture = 0.36f,
                tongueTipX = 0.36f,
                tongueTipY = 0.62f,
                tongueBladeX = 0.48f,
                tongueBladeY = 0.54f,
                tongueDorsumX = 0.62f,
                tongueDorsumY = 0.44f,
                tongueRootX = 0.74f,
                tongueRootY = 0.58f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.LAMINAR_OPEN,
                contactLabel = "Mid-high back tongue; soft lip cushion"
            )
        ),
        GenAmPhoneme(
            id = "uw_vowel",
            ipa = "/uː/",
            name = "Tense Goose Vowel",
            category = PhonemeCategory.VOWEL,
            exampleWord = "smooth",
            exampleIpa = "/smuːð/",
            placementCue = "Round and protrude your lips into a small circle while arching the back of the tongue high toward the soft palate.",
            airflowCue = "Sustained voiced stream through narrow rounded lips.",
            commonMistake = "Unrounding the lips makes the vowel sound flat or overly short.",
            contrastPhonemeId = "uh_vowel",
            contrastLabel = "Compare /uː/ vs /ʊ/",
            pose = ArticulationPose(
                jawDrop = 0.22f,
                lipRounding = 0.92f,
                lipAperture = 0.22f,
                tongueTipX = 0.40f,
                tongueTipY = 0.63f,
                tongueBladeX = 0.52f,
                tongueBladeY = 0.52f,
                tongueDorsumX = 0.65f,
                tongueDorsumY = 0.36f,
                tongueRootX = 0.75f,
                tongueRootY = 0.54f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.LAMINAR_OPEN,
                contactLabel = "High back arch + tight rounded lips"
            )
        ),
        GenAmPhoneme(
            id = "flap_t",
            ipa = "/ɾ/",
            name = "American Flap T/D",
            category = PhonemeCategory.STOP_FLAP,
            exampleWord = "water",
            exampleIpa = "/ˈwɑ.ɾɚ/",
            placementCue = "Flick the relaxed tongue tip once—ultra lightly—against the alveolar ridge between two vowels (like a soft quick /d/).",
            airflowCue = "Keep voicing continuous without building up a hard burst of air.",
            commonMistake = "Releasing an aspirated puff (/tʰ/) in 'water' or 'better' sounds stiff rather than natural GenAm.",
            contrastPhonemeId = "l_clear",
            contrastLabel = "Compare /ɾ/ vs /l/",
            pose = ArticulationPose(
                jawDrop = 0.28f,
                lipRounding = 0.10f,
                lipAperture = 0.34f,
                tongueTipX = 0.28f,
                tongueTipY = 0.33f,
                tongueBladeX = 0.41f,
                tongueBladeY = 0.48f,
                tongueDorsumX = 0.57f,
                tongueDorsumY = 0.54f,
                tongueRootX = 0.72f,
                tongueRootY = 0.64f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.PLOSIVE_TAP,
                contactLabel = "Ultra-brief ballistic tap on alveolar ridge"
            )
        ),
        GenAmPhoneme(
            id = "dark_l",
            ipa = "/ɫ/",
            name = "American Dark L",
            category = PhonemeCategory.RHOTIC,
            exampleWord = "people",
            exampleIpa = "/ˈpiː.pəɫ/",
            placementCue = "Touch the alveolar ridge lightly with the tip while simultaneously pulling the back of the tongue down and back toward the velum.",
            airflowCue = "Deep, resonant voiced airflow passing around both sides of the tongue.",
            commonMistake = "Keeping the tongue back too high creates a bright European 'L' instead of the warm American syllabic dark L.",
            contrastPhonemeId = "l_clear",
            contrastLabel = "Compare /ɫ/ vs Clear /l/",
            pose = ArticulationPose(
                jawDrop = 0.36f,
                lipRounding = 0.20f,
                lipAperture = 0.40f,
                tongueTipX = 0.29f,
                tongueTipY = 0.36f,
                tongueBladeX = 0.44f,
                tongueBladeY = 0.56f,
                tongueDorsumX = 0.65f,
                tongueDorsumY = 0.44f,
                tongueRootX = 0.76f,
                tongueRootY = 0.56f,
                velumOpen = 0.0f,
                voiced = true,
                airflowType = AirflowType.LAMINAR_OPEN,
                contactLabel = "Alveolar tip + velarized tongue back"
            )
        ),
        GenAmPhoneme(
            id = "ng_nasal",
            ipa = "/ŋ/",
            name = "Velar Nasal NG",
            category = PhonemeCategory.NASAL,
            exampleWord = "singing",
            exampleIpa = "/ˈsɪŋ.ɪŋ/",
            placementCue = "Seal the back of the tongue firmly up against the lowered soft palate (velum) while keeping the tongue tip resting low.",
            airflowCue = "Channel 100% of the voiced sound smoothly through the nasal passage without releasing a hard /g/.",
            commonMistake = "Adding a sharp /g/ release ('sing-guh') or substituting front /n/ ('singin').",
            contrastPhonemeId = "ih_vowel",
            contrastLabel = "Compare /ŋ/ vs /ɪ/",
            pose = ArticulationPose(
                jawDrop = 0.30f,
                lipRounding = 0.08f,
                lipAperture = 0.34f,
                tongueTipX = 0.28f,
                tongueTipY = 0.66f,
                tongueBladeX = 0.44f,
                tongueBladeY = 0.55f,
                tongueDorsumX = 0.64f,
                tongueDorsumY = 0.35f,
                tongueRootX = 0.74f,
                tongueRootY = 0.56f,
                velumOpen = 0.95f,
                voiced = true,
                airflowType = AirflowType.NASAL_CHANNEL,
                contactLabel = "Back of tongue seals soft palate; nasal airflow"
            )
        )
    )

    val phonemeById: Map<String, GenAmPhoneme> = phonemes.associateBy { it.id }

    fun resolveContrastPose(contrastId: String?): Pair<String, ArticulationPose>? {
        if (contrastId == null) return null
        phonemeById[contrastId]?.let { return "${it.ipa} ${it.name}" to it.pose }
        return contrastExtraPoses[contrastId]
    }

    val starterPhrases: List<PracticePhrase> = listOf(
        PracticePhrase(
            id = "phrase_th_01",
            title = "Dental Clarity Warmup",
            text = "I think thirty-three things are worth thanking them for.",
            ipa = "/aɪ θɪŋk ˈθɝ.ɾi θɹiː θɪŋz ɑɹ wɝθ ˈθæŋ.kɪŋ ðɛm fɔɹ/",
            stressGuide = "I THINK THIR-ty-THREE THINGS are WORTH THANK-ing them for.",
            targetPhonemeIds = listOf("th_voiceless", "th_voiced", "er_rhotic"),
            difficulty = "Foundation",
            category = PhonemeCategory.DENTAL
        ),
        PracticePhrase(
            id = "phrase_r_01",
            title = "American Rhotic Flow",
            text = "Robert rarely reads early morning reports in the library.",
            ipa = "/ˈɹɑ.bɚt ˈɹɛɹ.li ɹiːdz ˈɝ.li ˈmɔɹ.nɪŋ ɹɪˈpɔɹts ɪn ðə ˈlaɪ.bɹɛɹ.i/",
            stressGuide = "ROB-ert RARE-ly READS EAR-ly MORN-ing re-PORTS in the LI-brar-y.",
            targetPhonemeIds = listOf("r_american", "er_rhotic", "iy_vowel"),
            difficulty = "Core",
            category = PhonemeCategory.RHOTIC
        ),
        PracticePhrase(
            id = "phrase_flap_01",
            title = "Natural Flap T Cadence",
            text = "A little bottle of better water matters a lot on a hot Saturday.",
            ipa = "/ə ˈlɪ.ɾəɫ ˈbɑ.ɾəɫ əv ˈbɛ.ɾɚ ˈwɑ.ɾɚ ˈmæ.ɾɚz ə lɑt ɑn ə hɑt ˈsæ.ɾɚ.deɪ/",
            stressGuide = "A LIT-tle BOT-tle of BET-ter WA-ter MAT-ters a LOT on a HOT SAT-ur-day.",
            targetPhonemeIds = listOf("flap_t", "ae_vowel", "dark_l"),
            difficulty = "Core",
            category = PhonemeCategory.STOP_FLAP
        ),
        PracticePhrase(
            id = "phrase_vowel_01",
            title = "Tense vs. Lax Contrast",
            text = "Tim will leave this sheep on the green hill before evening.",
            ipa = "/tɪm wɪɫ liːv ðɪs ʃiːp ɑn ðə ɡɹiːn hɪɫ bɪˈfɔɹ ˈiːv.nɪŋ/",
            stressGuide = "TIM will LEAVE this SHEEP on the GREEN HILL be-FORE EVE-ning.",
            targetPhonemeIds = listOf("ih_vowel", "iy_vowel", "dark_l"),
            difficulty = "Foundation",
            category = PhonemeCategory.VOWEL
        ),
        PracticePhrase(
            id = "phrase_ae_01",
            title = "Open Jaw Ash Vowel",
            text = "The manager had a practical plan to hand back the sample.",
            ipa = "/ðə ˈmæ.nɪ.dʒɚ hæd ə ˈpɹæk.tɪ.kəɫ plæn tə hænd bæk ðə ˈsæm.pəɫ/",
            stressGuide = "The MAN-a-ger HAD a PRAC-ti-cal PLAN to HAND BACK the SAM-ple.",
            targetPhonemeIds = listOf("ae_vowel", "r_american", "dark_l"),
            difficulty = "Core",
            category = PhonemeCategory.VOWEL
        ),
        PracticePhrase(
            id = "phrase_uw_01",
            title = "Rounded Back Vowels",
            text = "She took a good look at the smooth blue wooden spoon.",
            ipa = "/ʃi tʊk ə ɡʊd lʊk æt ðə smuːð bluː ˈwʊ.dən spuːn/",
            stressGuide = "She TOOK a GOOD LOOK at the SMOOTH BLUE WOOD-en SPOON.",
            targetPhonemeIds = listOf("uh_vowel", "uw_vowel", "th_voiced"),
            difficulty = "Intermediate",
            category = PhonemeCategory.VOWEL
        ),
        PracticePhrase(
            id = "phrase_ng_01",
            title = "Velar Nasal Resonance",
            text = "Bringing strong morning energy keeps everything running smoothly.",
            ipa = "/ˈbɹɪŋ.ɪŋ stɹɔŋ ˈmɔɹ.nɪŋ ˈɛ.nɚ.dʒi kiːps ˈɛv.ɹi.θɪŋ ˈɹʌ.nɪŋ ˈsmuːð.li/",
            stressGuide = "BRING-ing STRONG MORN-ing EN-er-gy KEEPS EV-ery-thing RUN-ning SMOOTH-ly.",
            targetPhonemeIds = listOf("ng_nasal", "r_american", "th_voiceless"),
            difficulty = "Intermediate",
            category = PhonemeCategory.NASAL
        ),
        PracticePhrase(
            id = "phrase_pro_01",
            title = "Global Executive Clarity",
            text = "Rather than rushing through quarterly numbers, let's clarify our third priority.",
            ipa = "/ˈɹæ.ðɚ ðæn ˈɹʌ.ʃɪŋ θɹuː ˈkwɔɹ.ɾɚ.li ˈnʌm.bɚz lɛts ˈklɛɹ.ə.faɪ ˈaʊ.ɚ θɝd pɹaɪˈɔɹ.ə.ɾi/",
            stressGuide = "RATH-er than RUSH-ing through QUAR-ter-ly NUM-bers, let's CLAR-i-fy our THIRD pri-OR-i-ty.",
            targetPhonemeIds = listOf("th_voiced", "th_voiceless", "r_american", "flap_t"),
            difficulty = "Fluency",
            category = PhonemeCategory.RHOTIC
        )
    )
}
