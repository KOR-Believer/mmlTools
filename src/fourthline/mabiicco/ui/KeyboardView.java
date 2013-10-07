/*
 * Copyright (C) 2013 たんらる
 */

package fourthline.mabiicco.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JPanel;

import fourthline.mabiicco.midi.MabiDLS;

public class KeyboardView extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3850112420986284800L;

	private IInstSource instSource = null;
	
	/**
	 * Create the panel.
	 */
	public KeyboardView() {
		setSize(getPreferredSize());
		
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				playNote( e.getY() );
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				offNote();
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				playNote( e.getY() );
			}
		});
	}
	
	public void setInstSource(IInstSource source) {
		instSource = source;
	}

	public Dimension getPreferredSize() {
		return new Dimension(100, 649);
	}


	/**
	 * 1オクターブ 12 x 6
	 * 9オクターブ分つくると、648
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g.create();
		for (int i = 0; i <= 9; i++) {
			paintOctPianoLine(g2, i, (char)('0'+8-i));
		}

		g2.dispose();
	}


	private void paintOctPianoLine(Graphics g, int pos, char posText) {

		int white_wigth[] = { 10, 10, 10, 11, 10, 10, 11 };
		// ド～シのしろ鍵盤
		g.setColor(new Color(0.3f, 0.3f, 0.3f));

		int startY = 12*6*pos;
		int y = startY;
		for (int i = 0; i < white_wigth.length; i++) {
			g.drawRect(0, y, 40, white_wigth[i]);
			y += white_wigth[i];
		}
		// 黒鍵盤
		g.setColor(new Color(0.0f, 0.0f, 0.0f));
		g.fillRect(0, (0*10+5)+1+startY, 20, 6);
		g.fillRect(0, (1*10+5)+2+startY, 20, 6);
		g.fillRect(0, (2*10+5)+3+startY, 20, 6);
		g.fillRect(0, (4*10+5)+1+startY, 20, 6);
		g.fillRect(0, (5*10+5)+3+startY, 20, 6);
		g.setColor(new Color(0.3f, 0.3f, 0.3f));
		g.drawRect(0, (0*10+5)+1+startY, 20, 6);
		g.drawRect(0, (1*10+5)+2+startY, 20, 6);
		g.drawRect(0, (2*10+5)+3+startY, 20, 6);
		g.drawRect(0, (4*10+5)+1+startY, 20, 6);
		g.drawRect(0, (5*10+5)+3+startY, 20, 6);

		// グリッド
		y = startY;
		g.setColor(new Color(0.3f, 0.3f, 0.6f));
		g.drawLine(40, y, 100, y);
		for (int i = 1; i < 12; i++) {
			g.drawLine(60, i*6+y, 100, i*6+y);
		}

		// drawChars(char[] data, int offset, int length, int x, int y)
		char o_char[] = { 'o', posText };
		g.setFont(new Font("Arial", Font.PLAIN, 12));
		g.drawChars(o_char, 0, o_char.length, 42, startY+(12*6));
	}
	
	private int convertY2Note(int y) {
		int note = -1;
		if (y >= 0) {
			note = (9*12-(y/6)) -1 +12;
		}
		
		return note;
	}
	
	private void playNote(int y) {
		int note = convertY2Note( y );
		
		if (instSource != null) {
			int program = instSource.getInstProgram();
			MabiDLS.getInstance().changeProgram(program);
		}
		MabiDLS.getInstance().playNote(note);
	}
	
	private void offNote() {
		MabiDLS.getInstance().playNote(-1);
	}
}