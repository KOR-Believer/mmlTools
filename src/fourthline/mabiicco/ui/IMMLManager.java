/*
 * Copyright (C) 2013-2014 たんらる
 */

package fourthline.mabiicco.ui;

import fourthline.mmlTools.MMLEventList;
import fourthline.mmlTools.MMLScore;

/**
 *
 */
public interface IMMLManager {
	public MMLScore getMMLScore();
	public int getActiveTrackIndex();
	public MMLEventList getActiveMMLPart();
	public void updateActivePart(boolean generate);
	public void updateActiveTrackProgram(int trackIndex, int program, int songProgram);
	public int getActivePartProgram();
	public boolean selectTrackOnExistNote(int note, int tickOffset);
	
}
