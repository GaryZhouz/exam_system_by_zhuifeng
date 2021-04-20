package com.wzz.Util.CertificateUtil;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentStyle {
	// windowss下用这个
	private String TTFPath = "C:/WINDOWS/Fonts/SIMYOU.TTF";// 字体类型
	// linux下用这个
//	private String TTFPath = "/usr/share/fonts/dejavu/DejaVuSans.ttf";// 字体类型
	private float  fontSize = 12;//字体大小
	private BaseColor baseColor = new BaseColor(0, 0, 0);//默认是黑色
	private int style = Font.NORMAL;//字体样式
	private int alignment = Element.ALIGN_LEFT;

	public String getTTFPath() {
		return TTFPath;
	}
}
