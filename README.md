The RyzomDemos project demonstrates how to use assets exported
from the Ryzom Asset Repository by means of Alweth's `RyzomConverter`.

## BuildCharacter

The `BuildCharacter` application allows you to configure a humanoid character,
select an animation, and see the results in real time.
The number of possible body configurations exceeds 6 x 10^14.

  1. Install and convert the asset repository
     by following the suggested procedure
     at https://github.com/stephengold/RyzomConverter
  2. `cd ..`
  3. `git clone https://github.com/stephengold/RyzomDemos.git`
  4. `cd RyzomDemos`
  5. `./gradlew run`

The first time it is run,
`BuildCharacter` processes 1,963 exported files during initialization
and generates a summary file.
This may take 90 seconds or more; please be patient.
On successive runs, the summary file is used instead,
so initialization should complete much more quickly.

You control `BuildCharacter` using hotkeys:

 + H : show/hide the help overlay
 + up/down : cycle through the properties
 + left/right : cycle through values for the selected property
 + Num7/Num9 : skip back/forward past 6 values of the selected property
 + Num5 : randomize the selected property
 + R : randomize all 7 body-part properties
 + W/A/S/D/Q/Z : dolly forward, orbit left, dolly back, orbit right, rise, fall
 + comma : save the constructed model to files
 + PrtSc : save a screenshot to a file
 + M : show/hide the character's meshes
 + V : show/hide the skeleton overlay
 + F5 : show/hide the render statistics overlay
 + semicolon : show/hide the coordinate axes
 + . : pause/resume the animation
 + Esc : quit the application
 + C : dump the camera state to System.out
 + P : dump the scene graphs to System.out

You can also drag with the LMB (left mouse button)
to pan and/or tilt the camera.

<img height="360" src="https://i.imgur.com/wLikttk.png">

The screenshot is derived from Ryzom Asset Repository, licensed CC-BY-SA 3.0.
Alweth is acknowledged for authoring `RyzomConverter`
and providing it free of charge.

## DodgerGame

The `DodgerGame` application plays a simple game using the exported assets.

Unseen persons are shooting arrows at your avatar.
Your goal is to avoid the arrows by strafing to the left or right.
You earn one point for each arrow that misses your avatar.

    ./gradlew runDodgerGame

You control `DodgerGame` using hotkeys:

 + H : show/hide the help overlay
 + enter/G/Y : start a new game
 + left/A/Num4 : strafe left and then return to center
 + right/D/Num6 : strafe right and then return to center
 + Esc : quit the application
 + F5 : show/hide the render statistics overlay
 + PrtSc : save a screenshot to a file

<img height="360" src="https://i.imgur.com/iwmaQ8k.png">

The screenshot is derived from Ryzom Asset Repository, licensed CC-BY-SA 3.0.
Alweth is acknowledged for authoring `RyzomConverter`
and providing it free of charge.

## Statistics

The `Statistics` application derives statistics
from the exported assets and prints a report to `System.out`.

If the `Statistics` application doesn't find a summary file,
it should generate one.

    ./gradlew runStatistics
