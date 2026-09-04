package dev.ipf.whitenoise.model

/** Only the existing developer surface inserts this authored document into a chat. */
object MessageReadingExamples {
    val document = """
        # Notes from the trail

        **The route is ready.** Meet at *the east gate* and bring water.
        Here is the [route guide](https://example.org/trail). ~~The west gate~~ is closed.

        ## Before we leave
        - [x] Check the weather
        - [ ] Pack a warm layer
        - [ ] Share the plan

        > Leave enough time to stop and enjoy the view.

        | Stop | Distance | Plan |
        | :--- | ---: | :---: |
        | River | 2 km | Water |
        | Ridge | 5 km | Lunch |

        <details>
        <summary>Directions for the return</summary>
        Follow the river path to the village. Turn left at the old bridge.
        </details>

        ### Packing list
        1. Water
        2. Lunch
           - Fruit
           - Sandwiches
        3. Map

        ```text
        Start: east gate
        Return: old bridge
        ```

        ---

        ## Walking journal
    """.trimIndent() + "\n\n" + (1..60).joinToString("\n\n") { day ->
        "Day $day: We followed the trail through the trees, paused by the river, and returned before sunset. The quiet stretch above the bridge was the best place to rest."
    }
}
