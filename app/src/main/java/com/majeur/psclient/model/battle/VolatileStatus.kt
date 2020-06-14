package com.majeur.psclient.model.battle

import com.majeur.psclient.model.common.Colors

class VolatileStatus private constructor(val id: String, val label: String?, val color: Int) {

    companion object {
        @JvmStatic fun getForId(name: String?) = when (name) {
            "dynamax" -> VolatileStatus(name, "Dynamaxed", Colors.VOLATILE_GOOD)
            "throatchop" -> VolatileStatus(name, "Throat Chop", Colors.VOLATILE_BAD)
            "powertrick" -> VolatileStatus(name, "Power Trick", Colors.VOLATILE_NEUTRAL)
            "foresight", "miracleeye" -> VolatileStatus(name, "Identified", Colors.VOLATILE_BAD)
            "telekinesis" -> VolatileStatus(name, "Telekinesis", Colors.VOLATILE_NEUTRAL)
            "transform" -> VolatileStatus(name, "Transformed", Colors.VOLATILE_NEUTRAL)
            "confusion" -> VolatileStatus(name, "Confused", Colors.VOLATILE_BAD)
            "healblock" -> VolatileStatus(name, "Heal Block", Colors.VOLATILE_BAD)
            "yawn" -> VolatileStatus(name, "Drowsy", Colors.VOLATILE_BAD)
            "taunt" -> VolatileStatus(name, "Taunted", Colors.VOLATILE_BAD)
            "imprison" -> VolatileStatus(name, "Imprisoning", Colors.VOLATILE_GOOD)
            "disable" -> VolatileStatus(name, "Disabled", Colors.VOLATILE_BAD)
            "embargo" -> VolatileStatus(name, "Embargo", Colors.VOLATILE_BAD)
            "torment" -> VolatileStatus(name, "Tormented", Colors.VOLATILE_BAD)
            "ingrain" -> VolatileStatus(name, "Ingrained", Colors.VOLATILE_GOOD)
            "aquaring" -> VolatileStatus(name, "Aqua Ring", Colors.VOLATILE_GOOD)
            "stockpile1" -> VolatileStatus("stockpile", "Stockpile", Colors.VOLATILE_GOOD)
            "stockpile2" -> VolatileStatus("stockpile", "Stockpile×2", Colors.VOLATILE_GOOD)
            "stockpile3" -> VolatileStatus("stockpile", "Stockpile×3", Colors.VOLATILE_GOOD)
            "perish0" -> VolatileStatus("perish", null, Colors.VOLATILE_BAD)
            "perish1" -> VolatileStatus("perish", "Perish next turn", Colors.VOLATILE_BAD)
            "perish2" -> VolatileStatus("perish", "Perish in 2", Colors.VOLATILE_BAD)
            "perish3" -> VolatileStatus("perish", "Perish in 3", Colors.VOLATILE_BAD)
            "encore" -> VolatileStatus(name, "Encored", Colors.VOLATILE_BAD)
            "bide" -> VolatileStatus(name, "Bide", Colors.VOLATILE_GOOD)
            "attract" -> VolatileStatus(name, "Attracted", Colors.VOLATILE_BAD)
            "autotomize" -> VolatileStatus(name, "Lightened", Colors.VOLATILE_GOOD)
            "focusenergy" -> VolatileStatus(name, "+Crit rate", Colors.VOLATILE_GOOD)
            "curse" -> VolatileStatus(name, "Cursed", Colors.VOLATILE_BAD)
            "nightmare" -> VolatileStatus(name, "Nightmare", Colors.VOLATILE_BAD)
            "magnetrise" -> VolatileStatus(name, "Magnet Rise", Colors.VOLATILE_GOOD)
            "smackdown" -> VolatileStatus(name, "Smacked Down", Colors.VOLATILE_BAD)
            "substitute" -> VolatileStatus(name, "Substitute", Colors.VOLATILE_NEUTRAL)
            "lightscreen" -> VolatileStatus(name, "Light Screen", Colors.VOLATILE_GOOD)
            "reflect" -> VolatileStatus(name, "Reflect", Colors.VOLATILE_GOOD)
            "flashfire" -> VolatileStatus(name, "Flash Fire", Colors.VOLATILE_GOOD)
            "airballoon" -> VolatileStatus(name, "Balloon", Colors.VOLATILE_GOOD)
            "leechseed" -> VolatileStatus(name, "Leech Seed", Colors.VOLATILE_BAD)
            "slowstart" -> VolatileStatus(name, "Slow Start", Colors.VOLATILE_BAD)
            "noretreat" -> VolatileStatus(name, "No Retreat", Colors.VOLATILE_BAD)
            "octolock" -> VolatileStatus(name, "Octolock", Colors.VOLATILE_BAD)
            "mimic" -> VolatileStatus(name, "Mimic", Colors.VOLATILE_GOOD)
            "watersport" -> VolatileStatus(name, "Water Sport", Colors.VOLATILE_GOOD)
            "mudsport" -> VolatileStatus(name, "Mud Sport", Colors.VOLATILE_GOOD)
            "uproar" -> VolatileStatus(name, "Uproar", Colors.VOLATILE_NEUTRAL)
            "rage" -> VolatileStatus(name, "Rage", Colors.VOLATILE_NEUTRAL)
            "roost" -> VolatileStatus(name, "Landed", Colors.VOLATILE_NEUTRAL)
            "protect" -> VolatileStatus(name, "Protect", Colors.VOLATILE_GOOD)
            "quickguard" -> VolatileStatus(name, "Quick Guard", Colors.VOLATILE_GOOD)
            "wideguard" -> VolatileStatus(name, "Wide Guard", Colors.VOLATILE_GOOD)
            "craftyshield" -> VolatileStatus(name, "Crafty Shield", Colors.VOLATILE_GOOD)
            "matblock" -> VolatileStatus(name, "Mat Block", Colors.VOLATILE_GOOD)
            "maxguard" -> VolatileStatus(name, "Max Guard", Colors.VOLATILE_GOOD)
            "helpinghand" -> VolatileStatus(name, "Helping Hand", Colors.VOLATILE_GOOD)
            "magiccoat" -> VolatileStatus(name, "Magic Coat", Colors.VOLATILE_GOOD)
            "destinybond" -> VolatileStatus(name, "Destiny Bond", Colors.VOLATILE_GOOD)
            "snatch" -> VolatileStatus(name, "Snatch", Colors.VOLATILE_GOOD)
            "grudge" -> VolatileStatus(name, "Grudge", Colors.VOLATILE_GOOD)
            "charge" -> VolatileStatus(name, "Charge", Colors.VOLATILE_GOOD)
            "endure" -> VolatileStatus(name, "Endure", Colors.VOLATILE_GOOD)
            "focuspunch" -> VolatileStatus(name, "Focusing", Colors.VOLATILE_NEUTRAL)
            "shelltrap" -> VolatileStatus(name, "Trap set", Colors.VOLATILE_NEUTRAL)
            "powder" -> VolatileStatus(name, "Powder", Colors.VOLATILE_BAD)
            "electrify" -> VolatileStatus(name, "Electrify", Colors.VOLATILE_BAD)
            "ragepowder" -> VolatileStatus(name, "Rage Powder", Colors.VOLATILE_GOOD)
            "followme" -> VolatileStatus(name, "Follow Me", Colors.VOLATILE_GOOD)
            "instruct" -> VolatileStatus(name, "Instruct", Colors.VOLATILE_NEUTRAL)
            "beakblast" -> VolatileStatus(name, "Beak Blast", Colors.VOLATILE_NEUTRAL)
            "laserfocus" -> VolatileStatus(name, "Laser Focus", Colors.VOLATILE_GOOD)
            "spotlight" -> VolatileStatus(name, "Spotlight", Colors.VOLATILE_NEUTRAL)
            "bind" -> VolatileStatus(name, "Bind", Colors.VOLATILE_BAD)
            "clamp" -> VolatileStatus(name, "Clamp", Colors.VOLATILE_BAD)
            "firespin" -> VolatileStatus(name, "Fire Spin", Colors.VOLATILE_BAD)
            "infestation" -> VolatileStatus(name, "Infestation", Colors.VOLATILE_BAD)
            "magmastorm" -> VolatileStatus(name, "Magma Storm", Colors.VOLATILE_BAD)
            "sandtomb" -> VolatileStatus(name, "Sand Tomb", Colors.VOLATILE_BAD)
            "whirlpool" -> VolatileStatus(name, "Whirlpool", Colors.VOLATILE_BAD)
            "wrap" -> VolatileStatus(name, "Wrap", Colors.VOLATILE_BAD)
            else -> null
        }
    }
}
