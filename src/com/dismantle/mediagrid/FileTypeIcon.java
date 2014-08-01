package com.dismantle.mediagrid;

public class FileTypeIcon {
	private static final Object[][] Icon_MapTable = {
			{ ".zip", R.string.fa_archive },
			{ ".wav", R.string.fa_file_audio_o },
			{ ".xlsx", R.string.fa_file_excel_o },
			{ ".docx", R.string.fa_file_word_o },
			{ ".pptx", R.string.fa_file_powerpoint_o },
			{ ".png", R.string.fa_file_image_o },
			{ ".mp3", R.string.fa_file_audio_o },
			{ ".mp4", R.string.fa_file_movie_o },
			{ ".java", R.string.fa_file_code_o }

	};
	public static int getIcon(String posix)
	{
		posix = posix.toLowerCase();
		for (int i = 0; i < Icon_MapTable.length; i++) {
			if (posix.equals(Icon_MapTable[i][0]))
				return (Integer)Icon_MapTable[i][1];
		}
		return R.string.fa_file;
	}
}
