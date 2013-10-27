/*
 * Copyright (C) 2013 たんらる
 */

package fourthline.mmlTools;

public class MMLTempoEvent extends MMLEvent {

	private int tempo;
	public static final int META = 0x51;  /* MIDI meta: tempo */
	
	public MMLTempoEvent(int tempo, int tickOffset) {
		super(tickOffset);
		
		this.tempo = tempo;
	}
	
	public int getTempo() {
		return this.tempo;
	}
	
	public byte[] getMetaData() {
		byte[] retVal = { (byte)tempo };
		
		return retVal;
	}

	@Override
	public String toString() {
		return "[Tempo] " + tempo;
	}
}