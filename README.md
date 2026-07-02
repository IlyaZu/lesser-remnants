<!-- 
-- Copyright 2024-2026 Ilya Zushinskiy
--
-- Licensed under the GNU General Public License, Version 3 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at:
--
--    https://www.gnu.org/licenses/gpl-3.0.html
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-->

# Lesser Remnants

## Overview

Everyone seems to agree that Master of Orion is brilliant due to its simplicity and elegance, yet most 4X games since have only added features.
This is understandable as feature removals aren't exactly an exciting thing to put on the back of the box.
Fortunately, this is a hobbyist endeavour free any of such constraints and, as such, the project aims to answer "What if Remnants of the Precursors but less?" in gory detail.

To this end when a problem is found with the code/UI/story/game mechanics - the first solution to be considered will be removal.
If that is deemed not to be a valid solution, minimalistic changes to the aforementioned will be considered.
No new features are planned.

## Unobvious Changes

This is not the complete list of changes (see the git history for that) but a highlight of changes that might not be readily apparent.

### Removed Final War

One of great things about Master of Orion is that it realised that cleaning up the map after you had already won is really boring.
So the victory condition is outnumbering your opponents 2:1 (either by yourself or as a coalition) - thus removing the later boring third of the game.

Allowing AI players to rebel against the council in Remnants of the Precursors fundamentally undermines this.
This is now removed.

Likewise allowing the Human player to rebel against the council puts you back in the same boring degenerate state.
While I understand that this gives the player an opportunity for "revenge", which helps to make the experience of losing more palatable, I think there is value in leaving the sting of defeat un-balmed, which makes it a better teachable moment and a more memorable experience.
This is also now removed.

### Removed Alliance Victory

This win condition is redundant as:
* If you are a Galactic Council candidate, your allies can demonstrate their loyalty by voting for you. If they don't then I don't think you've won.
* If you are not a Galactic Council candidate, you are in a prime position to win via Council Alliance condition. If you can't then there is still a game to play.

### Reworked relations calculations

Previously each incident had an individual timer for how long it took effect.
This meant that if you did two (or three, or four...) bad things at the same time you would be forgiven in the same amount of time as if you did one.

This does not feel very satisfying to me because it creates a sense of the relationship being static.
i.e. Things drift back to the starting point very easily.
It also overemphasises the importance of big diplomatic events vs small continues events, in my opinion.

To this end, the relationship calculations have been completely reworked.
Now each incident contributes to the overall relationship score and the score, as a whole, drifts towards the base relationship at a "fixed" rate.
This means two bad events are going to be forgiven in twice the time (roughly) and small reoccurring incidents can build up to have a large impact.

The way AI responds to different levels of relationship score has also been adjusted.

### Reworked diplomatic incidents

A number of diplomatic incidents (things wat make relations go up and down) have been removed, added or changed beyond recognition.
For example, use of bio weapons no longer decrease you a relations with other Empires.
My advice would be to go in assuming things don't work in the same way they once did.

The intent of all of these changes is to simplify and to deepen the diplomatic game dynamics.

## Stability

While every commit should represent a working, slightly improved, version of the game no attempt is made to make saves backwards compatible.
In fact, I know I have already broken save compatibility many times.
This is a necessary sacrifice to make the kind of changes I want to make.
If you are playing this game as changes come out consider finishing a session before pulling changes.

## Requirements

The project is developed using the latest JDK (not necessarily the latest LTS JDK!).
This may lag somewhat, from time to time, but if something doesn't compile the first port of call should be to check for JDK updates.

## Binaries

There is currently no built, pre-packaged distribution of this game.
If you want to play it, you will have to pull the code and compile it yourself.
I have no objections to others building and distributing game.
In fact, this is your legal right.
