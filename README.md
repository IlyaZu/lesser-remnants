# Lesser Remnants

## Overview

Everyone seems to agree that Master of Orion is brilliant due to its simplicity and elegance, yet most 4X games since have only added features.
This is understandable as feature removals aren't exactly an exciting thing to put on the back of the box.
Fortunately, this is a hobbyist endeavour free of such constraints and, as such, the project aims to answer "What if Remnants of the Precursors but less?" in gory detail.

Expect mostly removals from code, UI, story and game mechanics. 
If not removed, an attempt will be made to tweak the afore mentioned to simply work better.
No new features are planned.

## Notable Changes

This is not the complete list of changes (see the git history for that) but a highlight of changes that might not be readily apparent. 

### Simplified Victory Conditions

<strong>Final War has been removed.</strong>

One of great things about Master of Orion is that it realised that cleaning up the map after you had already won is really boring. 
So the victory condition is outnumbering your opponents 2:1 (either by yourself or as a coalition) - thus removing the later boring third of the game.

Allowing AI players to rebel against the council in Remnants of the Precursors fundamentally undermines this. 
This is now removed.

Likewise allowing the Human player to rebel against the council put you back in the same boring degenerate state.
While I understand that this gives the player an opportunity for "revenge", which help to make the experience of losing more palatable, I think there is value in leaving the sting of defeat un-balmed, which makes it a better teachable moment and a more memorable experience. 
This is also now removed.

<strong>Alliance Victory has been removed.</strong>

This win condition is redundant as:
* If you are a Galactic Council candidate, your allies can demonstrate their loyalty by voting for you. If they don't then I don't think you've won.
* If you are not a Galactic Council candidate, you are in a prime position to win via Council Alliance condition. If you can't then there is still a game to play.

## Stability

While every commit should represent a working, slightly improved, version of the game no attempt is made to make saves backwards compatible.
In fact, I know I have already broken save compatibility many times. 
This is a necessary sacrifice to make the kind of changes I want to make.
If you are playing this game as changes come out consider finishing a session before pulling changes.

## Binaries

There is currently no built, pre-packaged distribution of this game. 
If you want to play it, you will have to pull the code and compile it yourself.
I have no objections to others building and distributing game.
In fact, this is your legal right.
