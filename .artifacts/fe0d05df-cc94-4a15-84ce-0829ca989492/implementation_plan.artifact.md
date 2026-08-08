# Implementation Plan - Beach Scene Surprises

Add fun interactive elements and surprises to the beach scene to make it more engaging.

## User Review Required

> [!TIP]
> - **The Crab**: A small crab will pop out of the sand near the towel when it's clicked, scuttle a bit, and hide again.
> - **Shimmering Sun**: The sun will have a subtle pulsating/shimmering effect to feel "hotter".
> - **Sand Castle**: Long-pressing on the sand will build a small sandcastle.

## Proposed Changes

### 1. Beach Scene UI & Animation

#### [MODIFY] [BeachScene.kt](file:///C:/Users/vahan/AndroidStudioProjects/Sunnyreminder/app/src/main/java/com/vahan/sunnyreminder/ui/BeachScene.kt)
- Add `sunPulse` animation to the `infiniteTransition`.
- Add state for `crabVisible` and `crabOffset`.
- Add state for `castleVisible` and `castlePosition`.
- Update `drawSun()` to use `sunPulse` for scaling the glow.
- Implement `drawCrab()` with a simple scuttling animation when triggered by the towel click.
- Implement `drawSandCastle()` and add `onLongPress` detection in `pointerInput`.

### 2. Interaction Logic

#### [MODIFY] [BeachScene.kt](file:///C:/Users/vahan/AndroidStudioProjects/Sunnyreminder/app/src/main/java/com/vahan/sunnyreminder/ui/BeachScene.kt)
- In `detectTapGestures`:
    - Towel tap: In addition to opening the calendar, trigger the crab surprise.
- Add `detectDragGestures` or `onLongPress` to handle the sandcastle building.

## Verification Plan

### Manual Verification
1. Run the app and trigger the alarm.
2. Observe the sun -> Verify it pulsates/shimmers.
3. Tap the towel -> Verify the calendar opens AND a crab appears and scuttles.
4. Long-press on a blank area of the sand -> Verify a sandcastle appears.
