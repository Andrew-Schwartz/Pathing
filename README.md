# Pathing
Draw bezier curves on FRC field, and generate CSVs a robot can follow.

## To Use
Click to place points the robot will go through, right click or control click to place points that will guide the bezier generated as the path. Each individual path consists of a starting point (left click), 0 or more guide points (right clicks), and an end point (right click). Points' coordinates (in inches) can be adjusted individually either by typing in the text fields or by clicking that point's menu dropdown and selecting "point moving mode." The first tab, labeled "Position," is where the paths are generated in terms of x-y position. The "Velocity" tab contains graphs that show the position, velocity, and acceleration of the robot in feet from the center of the robot as well as from the wheels on either side. These kinematic graphs are constrained such that they minimize the time to follow the path while observing the physical rules of max velocity and acceleration (max jerk is currently unimplemented since it is orders of magnitude more complicated and would result in relatively small increases in pathh following accuracy). 

There are two ways to save a path once generated. "Files" -> "Export" saves a csv file with the information for a Talon-SRX to follow the path. "Files" -> "Save Points" saves a csv file with information about each intercept and guide point needed to generate the current path(s). Both options save to "Pathing/src/resources/saves" by default, but the save directory for each can be changed with "Files" -> "Change (CSV/Save) Location," and are named "${pathname}_left.csv", "${pathname}_right.csv", or "${pathname}_save.csv".

#### Other Features
* Undo and redo with ctrl-z and ctrl-y respectively
* Open previously saved paths (end in "_save.csv")
* Set the background image
* Options for how to draw the paths of the left and right wheels: not at all, perpendicular to the center path, or according to the calculated heading and velocity of each point

## To Run
1. Clone or download this repository
2. Make sure you have Java 11 (see pdf)
3. In the new Pathing directory, run `./gradlew run` on the command line
