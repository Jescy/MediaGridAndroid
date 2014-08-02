package com.dismantle.mediagrid;

public class FileTypeIcon {
	private static final Object[][] Icon_MapTable = {
			{ ".zip", R.string.fa_file_archive_o },
			{ ".rar", R.string.fa_file_archive_o },
			{ ".7z", R.string.fa_file_archive_o },
			{ ".gz", R.string.fa_file_archive_o },
			{ ".tar", R.string.fa_file_archive_o },
			
			{ ".mp3", R.string.fa_file_audio_o },
			{ ".wav", R.string.fa_file_audio_o },
			{ ".wma", R.string.fa_file_audio_o },
			
			{ ".xlsx", R.string.fa_file_excel_o },
			{ ".xls", R.string.fa_file_excel_o },
			
			{ ".docx", R.string.fa_file_word_o },
			{ ".doc", R.string.fa_file_word_o },
			
			{ ".pptx", R.string.fa_file_powerpoint_o },
			{ ".ppt", R.string.fa_file_powerpoint_o },
			
			{ ".png", R.string.fa_file_image_o },
			{ ".bmp", R.string.fa_file_image_o },
			{ ".gif", R.string.fa_file_image_o },
			{ ".jpg", R.string.fa_file_image_o },
			{ ".pcm", R.string.fa_file_image_o },
			
			{ ".mp4", R.string.fa_file_movie_o },
			{ ".mkv", R.string.fa_file_movie_o },
			{ ".avi", R.string.fa_file_movie_o },
			{ ".flv", R.string.fa_file_movie_o },
			{ ".rmvb", R.string.fa_file_movie_o },
			
			{ ".java", R.string.fa_file_code_o },
			{ ".py", R.string.fa_file_code_o },
			{ ".c", R.string.fa_file_code_o },
			{ ".cpp", R.string.fa_file_code_o },
			{ ".pl", R.string.fa_file_code_o },
			{ ".m", R.string.fa_file_code_o },
			{ ".h", R.string.fa_file_code_o },
			
			{ ".pdf", R.string.fa_file_pdf_o }

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
