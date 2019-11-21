The RyzomDemos project demonstrates how to use assets imported
from the Ryzom Asset Repository by means of Alweth's `RyzomConverter`.

## BuildCharacter

The `BuildCharacter` application allows you to configure a humanoid character,
select an animation, and see the results in real time.
The number of possible body configurations exceeds 6 x 10^11.

  1. Install and convert the asset repository
     by following the suggested procedure
     at https://github.com/stephengold/RyzomConverter
  2. `cd ..`
  3. `git clone https://github.com/stephengold/RyzomDemos.git`
  4. `cd RyzomDemos`
  5. `./gradlew run`

`BuildCharacter` processes 701 files during initialization.
This may take 10 seconds or more; please be patient.

You control `BuildCharacter` using hotkeys:

 + H : show/hide the help overlay
 + up/down : cycle through the properties
 + left/right : cycle through values for the selected property
 + Num5 : randomize the selected property
 + R : randomize all 7 body-part properties
 + W/A/S/D/Q/Z : dolly forward, orbit left, dolly back, orbit right, rise, fall
 + comma : save the constructed model to files
 + M : show/hide the character's meshes
 + V : show/hide the skeleton overlay
 + . : pause/resume the animation
 + Esc : quit the application
 + F5 : show/hide the statistics overlay
 + C : dump the camera state to System.out
 + P : dump the scene graphs to System.out

You can also drag with the LMB (left mouse button)
to pan and/or tilt the camera.

<img height="360" src="https://i.imgur.com/wVcItj0.jpg">

The screenshot is derived from Ryzom Asset Repository, licensed CC-BY-SA 3.0.
Alweth is acknowledged for authoring `RyzomConverter`
and providing it free of charge.

## Statistics

The `Statistics` application calculates statistics
related to the imported assets and prints a report to `System.out`.
