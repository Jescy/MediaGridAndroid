package com.dismantle.mediagrid;

/**
 * get FontAwesome icon and color for a certain file extension.
 * @author Jescy
 *
 */
public class FileTypeIcon {
	/**
	 * map table from extension to icon and color.
	 */
	private static final Object[][] Icon_MapTable = {
			{ ".zip", R.string.fa_file_archive_o, "#FFFF00" },
			{ ".rar", R.string.fa_file_archive_o, "#FFFF00" },
			{ ".7z", R.string.fa_file_archive_o, "#FFFF00" },
			{ ".gz", R.string.fa_file_archive_o, "#FFFF00" },
			{ ".tar", R.string.fa_file_archive_o, "#FFFF00" },

			{ ".mp3", R.string.fa_file_audio_o, "#CCCCCC" },
			{ ".wav", R.string.fa_file_audio_o, "#CCCCCC" },
			{ ".wma", R.string.fa_file_audio_o, "#CCCCCC" },

			{ ".xlsx", R.string.fa_file_excel_o, "#" },
			{ ".xls", R.string.fa_file_excel_o, "#" },

			{ ".docx", R.string.fa_file_word_o, "#99CCFF" },
			{ ".doc", R.string.fa_file_word_o, "#99CCFF" },

			{ ".pptx", R.string.fa_file_powerpoint_o, "#FF9900" },
			{ ".ppt", R.string.fa_file_powerpoint_o, "#FF9900" },

			{ ".png", R.string.fa_file_image_o, "#0099CC" },
			{ ".bmp", R.string.fa_file_image_o, "#0099CC" },
			{ ".gif", R.string.fa_file_image_o, "#0099CC" },
			{ ".jpg", R.string.fa_file_image_o, "#0099CC" },
			{ ".pcm", R.string.fa_file_image_o, "#0099CC" },

			{ ".mp4", R.string.fa_file_movie_o, "#" },
			{ ".mkv", R.string.fa_file_movie_o, "#" },
			{ ".avi", R.string.fa_file_movie_o, "#" },
			{ ".flv", R.string.fa_file_movie_o, "#" },
			{ ".rmvb", R.string.fa_file_movie_o, "#" },

			{ ".java", R.string.fa_file_code_o, "#" },
			{ ".py", R.string.fa_file_code_o, "#" },
			{ ".c", R.string.fa_file_code_o, "#" },
			{ ".cpp", R.string.fa_file_code_o, "#" },
			{ ".pl", R.string.fa_file_code_o, "#" },
			{ ".m", R.string.fa_file_code_o, "#" },
			{ ".h", R.string.fa_file_code_o, "#" },

			{ ".pdf", R.string.fa_file_pdf_o, "#F00000" }

	};

	/**
	 * get icon by suffix
	 * @param suffix suffix of file.
	 * @return icon
	 */
	public static int getIcon(String suffix) {
		suffix = suffix.toLowerCase();
		for (int i = 0; i < Icon_MapTable.length; i++) {
			if (suffix.equals(Icon_MapTable[i][0]))
				return (Integer) Icon_MapTable[i][1];
		}
		return R.string.fa_file;
	}
	/**
	 * get color by suffix.
	 * @param suffix suffix of file
	 * @return color
	 */
	public static String getColor(String suffix) {
		suffix = suffix.toLowerCase();
		for (int i = 0; i < Icon_MapTable.length; i++) {
			if (suffix.equals(Icon_MapTable[i][0]))
			{
				String color = (String) Icon_MapTable[i][2];
				if(color.length()<7)
					return "#000000";
				return color;
			}
		}
		return "#000000";
	}
}
