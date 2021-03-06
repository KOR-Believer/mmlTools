/*
 * Copyright (C) 2014 たんらる
 */

package fourthline.mabiicco;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import fourthline.mabiicco.midi.MabiDLS;
import fourthline.mabiicco.ui.About;
import fourthline.mabiicco.ui.MMLImportPanel;
import fourthline.mabiicco.ui.MMLScorePropertyPanel;
import fourthline.mabiicco.ui.MMLSeqView;
import fourthline.mabiicco.ui.MainFrame;
import fourthline.mabiicco.ui.editor.MMLTranspose;
import fourthline.mmlTools.MMLScore;
import fourthline.mmlTools.UndefinedTickException;
import fourthline.mmlTools.parser.IMMLFileParser;
import fourthline.mmlTools.parser.MMLFile;
import fourthline.mmlTools.parser.MMLParseException;
import fourthline.mmlTools.parser.MMSFile;

public final class ActionDispatcher implements ActionListener, IFileStateObserver, IEditStateObserver {

	private MainFrame mainFrame;
	private MMLSeqView mmlSeqView;
	private IFileState fileState;
	private IEditState editState;

	// action commands
	public static final String VIEW_SCALE_UP = "view_scale_up";
	public static final String VIEW_SCALE_DOWN = "view_scale_down";
	public static final String PLAY = "play";
	public static final String STOP = "stop";
	public static final String PAUSE = "pause";
	public static final String FILE_OPEN = "fileOpen";
	public static final String NEW_FILE = "newFile";
	public static final String RELOAD_FILE = "reloadFile";
	public static final String QUIT = "quit";
	public static final String ADD_TRACK = "addTrack";
	public static final String REMOVE_TRACK = "removeTrack";
	public static final String TRACK_PROPERTY = "trackProperty";
	public static final String SET_START_POSITION = "setStartPosition";
	public static final String INPUT_FROM_CLIPBOARD = "inputFromClipboard";
	public static final String OUTPUT_TO_CLIPBOARD = "outputToClipboard";
	public static final String UNDO = "undo";
	public static final String REDO = "redo";
	public static final String SAVE_FILE = "save_file";
	public static final String SAVEAS_FILE = "saveas_file";
	public static final String CUT = "cut";
	public static final String COPY = "copy";
	public static final String PASTE = "paste";
	public static final String DELETE = "delete";
	public static final String SCORE_PROPERTY = "score_property";
	public static final String NEXT_TIME = "next_time";
	public static final String PREV_TIME = "prev_time";
	public static final String PART_CHANGE = "part_change";
	public static final String CHANGE_NOTE_HEIGHT_INT = "change_note_height_";
	public static final String ADD_MEASURE = "add_measure";
	public static final String REMOVE_MEASURE = "remove_measure";
	public static final String ADD_BEAT = "add_beat";
	public static final String REMOVE_BEAT = "remove_beat";
	public static final String NOTE_PROPERTY = "note_property";
	public static final String TRANSPOSE = "transpose";
	public static final String ABOUT = "about";
	public static final String MIDI_EXPORT = "midi_export";
	public static final String FILE_IMPORT = "file_import";
	public static final String CLEAR_DLS = "clear_dls";
	public static final String SELECT_ALL = "select_all";

	private final HashMap<String, Runnable> actionMap = new HashMap<>();

	private File openedFile = null;

	private final FileFilter mmsFilter = new FileNameExtensionFilter(AppResource.appText("file.mms"), "mms");
	private final FileFilter mmiFilter = new FileNameExtensionFilter(AppResource.appText("file.mmi"), "mmi");
	private final FileFilter mmlFilter = new FileNameExtensionFilter(AppResource.appText("file.mml"), "mml");
	private final FileFilter allFilter = new FileNameExtensionFilter(AppResource.appText("file.all"), "mmi", "mms", "mml");
	private final FileFilter midFilter = new FileNameExtensionFilter(AppResource.appText("file.mid"), "mid");

	private final JFileChooser openFileChooser = new JFileChooser();
	private final JFileChooser saveFileChooser = new JFileChooser();
	private final JFileChooser exportFileChooser = new JFileChooser();

	private static ActionDispatcher instance = null;
	public static ActionDispatcher getInstance() {
		if (instance == null) {
			instance = new ActionDispatcher();
		}
		return instance;
	}

	private ActionDispatcher() {
	}

	public ActionDispatcher setMainFrame(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
		this.mmlSeqView = mainFrame.getMMLSeqView();
		this.fileState = this.mmlSeqView.getFileState();
		this.editState = this.mmlSeqView.getEditState();

		this.fileState.setFileStateObserver(this);
		this.editState.setEditStateObserver(this);
		return this;
	}

	public void initialize() {
		initializeFileChooser();
		initializeActionMap();
	}

	private void initializeFileChooser() {
		openFileChooser.addChoosableFileFilter(allFilter);
		openFileChooser.addChoosableFileFilter(mmiFilter);
		openFileChooser.addChoosableFileFilter(mmsFilter);
		openFileChooser.addChoosableFileFilter(mmlFilter);
		saveFileChooser.addChoosableFileFilter(mmiFilter);
		exportFileChooser.addChoosableFileFilter(midFilter);
	}

	private void initializeActionMap() {
		actionMap.put(VIEW_SCALE_UP, () -> {
			mmlSeqView.expandPianoViewWide();
			mmlSeqView.repaint();
		});
		actionMap.put(VIEW_SCALE_DOWN, () -> {
			mmlSeqView.reducePianoViewWide();
			mmlSeqView.repaint();
		});
		actionMap.put(STOP, () -> {
			MabiDLS.getInstance().getSequencer().stop();
			mainFrame.enableNoplayItems();
		});
		actionMap.put(PAUSE, this::pauseAction);
		actionMap.put(FILE_OPEN, () -> {
			if (checkCloseModifiedFileState()) {
				openMMLFileAction();
			}
		});
		actionMap.put(NEW_FILE, () -> {
			if (checkCloseModifiedFileState()) {
				newMMLFileAction();
			}
		});
		actionMap.put(RELOAD_FILE, this::reloadMMLFileAction);
		actionMap.put(QUIT, () -> {
			//  閉じる前に、変更が保存されていなければダイアログ表示する.
			if (checkCloseModifiedFileState()) {
				System.exit(0);
			}
		});
		actionMap.put(ADD_TRACK, () -> mmlSeqView.addMMLTrack(null));
		actionMap.put(REMOVE_TRACK, mmlSeqView::removeMMLTrack);
		actionMap.put(TRACK_PROPERTY, () -> mmlSeqView.editTrackPropertyAction(mainFrame));
		actionMap.put(SET_START_POSITION, mmlSeqView::setStartPosition);
		actionMap.put(PLAY, this::playAction);
		actionMap.put(INPUT_FROM_CLIPBOARD, () -> mmlSeqView.inputClipBoardAction(mainFrame));
		actionMap.put(OUTPUT_TO_CLIPBOARD, () -> mmlSeqView.outputClipBoardAction(mainFrame));
		actionMap.put(UNDO, mmlSeqView::undo);
		actionMap.put(REDO, mmlSeqView::redo);
		actionMap.put(SAVE_FILE, () -> saveMMLFile(openedFile));
		actionMap.put(SAVEAS_FILE, this::saveAsMMLFileAction);
		actionMap.put(CUT, editState::selectedCut);
		actionMap.put(COPY, editState::selectedCopy);
		actionMap.put(PASTE, this::editPasteAction);
		actionMap.put(DELETE, editState::selectedDelete);
		actionMap.put(SCORE_PROPERTY, this::scorePropertyAction);
		actionMap.put(NEXT_TIME, () -> mmlSeqView.nextStepTimeTo(true));
		actionMap.put(PREV_TIME, () -> mmlSeqView.nextStepTimeTo(false));
		actionMap.put(PART_CHANGE, () -> mmlSeqView.partChange(mainFrame));
		actionMap.put(ADD_MEASURE, this::addMeasure);
		actionMap.put(REMOVE_MEASURE, this::removeMeasure);
		actionMap.put(ADD_BEAT, this::addBeat);
		actionMap.put(REMOVE_BEAT, this::removeBeat);
		actionMap.put(NOTE_PROPERTY, editState::noteProperty);
		actionMap.put(TRANSPOSE, () -> new MMLTranspose().execute(mainFrame, mmlSeqView));
		actionMap.put(ABOUT, () -> new About().show(mainFrame));
		actionMap.put(MIDI_EXPORT, this::midiExportAction);
		actionMap.put(FILE_IMPORT, this::fileImportAction);
		actionMap.put(CLEAR_DLS, this::clearDLSInformation);
		actionMap.put(SELECT_ALL, this::selectAll);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		if (command.startsWith(CHANGE_NOTE_HEIGHT_INT)) {
			int index = Integer.parseInt( command.substring(CHANGE_NOTE_HEIGHT_INT.length()) );
			mmlSeqView.setPianoRollHeightScaleIndex(index);
			MabiIccoProperties.getInstance().setPianoRollViewHeightScaleProperty(index);
		} else {
			Runnable func = actionMap.get(command);
			if (func != null) {
				func.run();
			}
		}
	}

	private MMLScore fileParse(File file) {
		MMLScore score = null;
		try {
			IMMLFileParser fileParser;
			if (file.toString().endsWith(".mms")) {
				fileParser = new MMSFile();
			} else if (file.toString().endsWith(".mml")) {
				fileParser = new MMLFile();
			} else {
				fileParser = new MMLScore();
			}
			score = fileParser.parse(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(mainFrame, 
					AppResource.appText("error.read"), 
					AppResource.appText("error.nofile"), 
					JOptionPane.WARNING_MESSAGE);
		} catch (MMLParseException e) {
			JOptionPane.showMessageDialog(mainFrame, 
					AppResource.appText("error.read"), 
					AppResource.appText("error.invalid_file"), JOptionPane.WARNING_MESSAGE);
		}
		return score;
	}

	public void openMMLFile(File file) {
		MMLScore score = fileParse(file);
		if (score != null) {
			mmlSeqView.setMMLScore(score);

			openedFile = file;
			notifyUpdateFileState();
			MabiIccoProperties.getInstance().setRecentFile(file.getPath());
			MabiDLS.getInstance().all();
		}
	}

	public void reloadMMLFileAction() {
		if (MabiDLS.getInstance().getSequencer().isRunning()) {
			return;
		}

		if (openedFile != null) {
			if (fileState.isModified()) {
				int status = JOptionPane.showConfirmDialog(mainFrame, 
						AppResource.appText("message.throw"), 
						"", 
						JOptionPane.YES_NO_OPTION, 
						JOptionPane.QUESTION_MESSAGE);
				if (status == JOptionPane.YES_OPTION) {
					openMMLFile(openedFile);
				}
			}
		}
	}

	/**
	 * @param file
	 * @return　保存に成功した場合は true, 失敗した場合は false を返す.
	 */
	private boolean saveMMLFile(File file) {
		try {
			// generateできないデータは保存させない.
			mmlSeqView.getMMLScore().generateAll();
			mmlSeqView.updateActivePart(false);
			FileOutputStream outputStream = new FileOutputStream(file);
			mmlSeqView.getMMLScore().writeToOutputStream(outputStream);
			mainFrame.setTitleAndFileName(file.getName());
			fileState.setOriginalBase();
			notifyUpdateFileState();
			MabiIccoProperties.getInstance().setRecentFile(file.getPath());
		} catch (UndefinedTickException | IOException e) {
			return false;
		}
		return true;
	}

	private void newMMLFileAction() {
		openedFile = null;
		mmlSeqView.initializeMMLTrack();
		notifyUpdateFileState();
		MabiDLS.getInstance().all();
	}

	private void openMMLFileAction() {
		if (MabiDLS.getInstance().getSequencer().isRunning()) {
			return;
		}

		SwingUtilities.invokeLater(() -> {
			File file = fileOpenDialog();
			if (file != null) {
				openMMLFile(file);
			}
		});
	}

	private File fileOpenDialog() {
		String recentPath = MabiIccoProperties.getInstance().getRecentFile();
		openFileChooser.setCurrentDirectory(new File(recentPath));
		openFileChooser.setFileFilter(allFilter);
		openFileChooser.setAcceptAllFileFilterUsed(false);
		int status = openFileChooser.showOpenDialog(mainFrame);
		if (status == JFileChooser.APPROVE_OPTION) {
			File file = openFileChooser.getSelectedFile();
			return file;
		}
		return null;
	}

	private void saveAsMMLFileAction() {
		SwingUtilities.invokeLater(() -> {
			showDialogSaveFile();
			notifyUpdateFileState();
		});
	}

	/**
	 * ファイル保存のためのダイアログ表示.
	 * @return 保存ファイル, 保存ファイルがなければnull.
	 */
	private File showSaveDialog(JFileChooser fileChooser, String suffix) {
		String recentPath = MabiIccoProperties.getInstance().getRecentFile();
		if (openedFile != null) {
			fileChooser.setSelectedFile(openedFile);
		} else {
			fileChooser.setCurrentDirectory(new File(recentPath));
		}
		fileChooser.setAcceptAllFileFilterUsed(false);
		int status = fileChooser.showSaveDialog(mainFrame);
		if (status == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (!file.toString().endsWith("."+suffix)) {
				file = new File(file+"."+suffix);
			}

			status = JOptionPane.YES_OPTION;
			if (file.exists()) {
				// すでにファイルが存在する場合の上書き警告表示.
				status = JOptionPane.showConfirmDialog(mainFrame, AppResource.appText("message.override"), "", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			}
			if (status == JOptionPane.YES_OPTION) {
				return file;
			}
		}
		return null;
	}

	/**
	 * 別名保存のダイアログ表示
	 * @return 保存した場合は trueを返す.
	 */
	private boolean showDialogSaveFile() {
		saveFileChooser.setFileFilter(mmiFilter);
		File file = showSaveDialog(saveFileChooser, "mmi");
		if (file != null) {
			if (saveMMLFile(file)) {
				openedFile = file;
			} else {
				JOptionPane.showMessageDialog(mainFrame, AppResource.appText("fail.saveFile"), "ERROR", JOptionPane.ERROR_MESSAGE);
			}
			return true;
		}
		return false;
	}

	/**
	 * MIDI-exportダイアログ表示
	 * @return 保存した場合は trueを返す.
	 */
	private void midiExportAction() {
		File file = showSaveDialog(exportFileChooser, "mid");
		if (file != null) {
			try {
				MidiSystem.write(MabiDLS.getInstance().createSequence(mmlSeqView.getMMLScore()), 1, file);
			} catch (IOException | InvalidMidiDataException e) {
				JOptionPane.showMessageDialog(mainFrame, e.getLocalizedMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * ファイルからTrack単位のインポートを行う.
	 */
	private void fileImportAction() {
		File file = fileOpenDialog();
		if (file != null) {
			MMLScore score = fileParse(file);
			if (score != null) {
				new MMLImportPanel(mainFrame, score, mmlSeqView).showDialog();
			}
		}
	}

	/**
	 * ファイルの変更状態をみて、アプリケーション終了ができるかどうかをチェックする.
	 * @return 終了できる状態であれば、trueを返す.
	 */
	private boolean checkCloseModifiedFileState() {
		if (!fileState.isModified()) {
			// 保存が必要な変更なし.
			return true;
		}

		// 保存するかどうかのダイアログ表示
		int status = JOptionPane.showConfirmDialog(mainFrame, AppResource.appText("message.modifiedClose"), "", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (status == JOptionPane.CANCEL_OPTION) {
			return false;
		} else if (status == JOptionPane.NO_OPTION) {
			return true;
		}

		if (openedFile == null) {
			// 新規ファイルなので、別名保存.
			if (showDialogSaveFile()) {
				return true;
			}
		} else {
			if (isSupportedSaveFile()) {
				// 上書き保存可.
				saveMMLFile(openedFile);
				return true;
			} else if (showDialogSaveFile()) {
				// ファイルOpenされているが、サポート外なので別名保存.
				return true;
			}
		}

		return false;
	}

	private boolean isSupportedSaveFile() {
		if (openedFile != null) {
			if (openedFile.getName().endsWith(".mmi")) {
				return true;
			}
		}

		return false;
	}

	private void scorePropertyAction() {
		MMLScorePropertyPanel propertyPanel = new MMLScorePropertyPanel();
		propertyPanel.showDialog(mainFrame, mmlSeqView.getMMLScore(), mmlSeqView.getFileState());
		mmlSeqView.repaint();
	}

	private void editPasteAction() {
		long startTick = mmlSeqView.getEditSequencePosition();
		editState.paste(startTick);
	}

	private void pauseAction() {
		MabiDLS.getInstance().getSequencer().stop();
		mmlSeqView.pauseTickPosition();
		mainFrame.enableNoplayItems();
	}

	private void playAction() {
		if (MabiDLS.getInstance().getSequencer().isRunning()) {
			pauseAction();
		} else {
			mmlSeqView.startSequence();
			mainFrame.disableNoplayItems();
		}
	}

	private void clearDLSInformation() {
		MabiIccoProperties properties = MabiIccoProperties.getInstance();
		StringBuilder sb = new StringBuilder(AppResource.appText("message.clear_dls"));
		properties.getDlsFile().stream().forEach(t -> {
			sb.append("\n * ").append(t.getName());
		});
		int status = JOptionPane.showConfirmDialog(mainFrame, 
				sb.toString(), 
				AppResource.appText("menu.clear_dls"), 
				JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.WARNING_MESSAGE);
		if (status == JOptionPane.OK_OPTION) {
			properties.setDlsFile(null);
			System.out.println("clearDLSInformation");
		}
	}

	private void selectAll() {
		editState.selectAll();
		mmlSeqView.repaint();
		notifyUpdateEditState();
	}

	private void addMeasure() {
		mmlSeqView.addTicks( mmlSeqView.getMMLScore().getMeasureTick() );
	}

	private void addBeat() {
		mmlSeqView.addTicks( mmlSeqView.getMMLScore().getBeatTick() );
	}

	private void removeMeasure() {
		mmlSeqView.removeTicks( mmlSeqView.getMMLScore().getMeasureTick() );
	}

	private void removeBeat() {
		mmlSeqView.removeTicks( mmlSeqView.getMMLScore().getBeatTick() );
	}

	@Override
	public void notifyUpdateFileState() {
		mainFrame.setCanSaveFile(false);
		mainFrame.setTitleAndFileName(null);
		mainFrame.setCanReloadFile(false);
		if (openedFile != null) {
			if (fileState.isModified()) {
				// 上書き保存有効
				if (isSupportedSaveFile()) {
					mainFrame.setCanSaveFile(true);
				}
				mainFrame.setTitleAndFileName(openedFile.getName()+" "+AppResource.appText("file.modified"));
				mainFrame.setCanReloadFile(true);
			} else {
				mainFrame.setTitleAndFileName(openedFile.getName());
			}
		}

		// undo-UI更新
		mainFrame.setCanUndo(fileState.canUndo());

		// redo-UI更新
		mainFrame.setCanRedo(fileState.canRedo());
	}

	@Override
	public void notifyUpdateEditState() {
		mainFrame.setSelectedEdit(editState.hasSelectedNote());
		mainFrame.setPasteEnable(editState.canPaste());
	}
}
