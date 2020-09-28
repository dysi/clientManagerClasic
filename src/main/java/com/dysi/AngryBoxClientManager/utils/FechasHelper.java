package com.dysi.AngryBoxClientManager.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class FechasHelper {

	public static final Date getDateNowSumDays(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, days);
		Date dateBefore30Days = cal.getTime();
		return dateBefore30Days;
	}

	public static final Date getFechaMaximaDelPeriodoDate(int anio, int mes) {

		Calendar fechaMax = Calendar.getInstance();
		fechaMax.set(anio, mes, 1, 0, 0);
		fechaMax.set(Calendar.HOUR_OF_DAY, 0);
		fechaMax.clear(Calendar.MINUTE);
		fechaMax.clear(Calendar.SECOND);
		fechaMax.clear(Calendar.MILLISECOND);
		fechaMax.add(Calendar.MILLISECOND, -1); // me trae la ultima fecha
												// posible del dia anterior

		return fechaMax.getTime();
	}

	public static final Calendar getFechaMaximaDelPeriodoCalendar(int anio, int mes) {
		Calendar fechaMax = Calendar.getInstance();
		fechaMax.set(anio, mes, 1, 0, 0);
		fechaMax.set(Calendar.HOUR_OF_DAY, 0);
		fechaMax.set(Calendar.MINUTE, 0);
		fechaMax.set(Calendar.SECOND, 0);
		fechaMax.set(Calendar.MILLISECOND, 0);
		fechaMax.add(Calendar.MILLISECOND, -1); // me trae la ultima fecha
												// posible del dia anterior

		return fechaMax;
	}

	public static final Date getFechaMinimaDelPeriodoDate(int anio, int mes) {

		Calendar fechaMin = Calendar.getInstance();
		fechaMin.set(anio, mes - 1, 1, 0, 0);
		fechaMin.set(Calendar.HOUR_OF_DAY, 0);
		fechaMin.set(Calendar.MINUTE, 0);
		fechaMin.set(Calendar.SECOND, 0);
		fechaMin.set(Calendar.MILLISECOND, 0);

		return fechaMin.getTime();
	}

	public static final Calendar getFechaMinimaDelPeriodoCalendar(int anio, int mes) {

		Calendar fechaMin = Calendar.getInstance();
		fechaMin.set(anio, mes - 1, 1, 0, 0);
		fechaMin.set(Calendar.HOUR_OF_DAY, 0);
		fechaMin.clear(Calendar.MINUTE);
		fechaMin.clear(Calendar.SECOND);
		fechaMin.clear(Calendar.MILLISECOND);

		return fechaMin;
	}
	
	public static final double getDiferenciaDias(Calendar fechaInicial, Calendar fechaFinal) {
		if(fechaInicial == null || fechaFinal == null) {
			return -99999d;
		}
		long seconds = (fechaFinal.getTimeInMillis() - fechaInicial.getTimeInMillis()) / 1000;
		double hours = (seconds / 3600);
		double dias = (hours / 24);
		return dias;
	}

	public static final long getDiferenciaSegundos(Calendar fechaInicial, Calendar fechaFinal) {
		long seconds = (fechaFinal.getTimeInMillis() - fechaInicial.getTimeInMillis()) / 1000;
		// int hours = (int) (seconds / 3600);
		return seconds;
	}

	public static final String getStringDate(Calendar calendar) {

		DateFormat formatter;
		formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
		return formatter.format(calendar.getTime());

	}

	public static final String getStringDateNoHHmmss(Calendar calendar) {

		DateFormat formatter;
		formatter = new SimpleDateFormat("yyyy-MM-dd");
		return formatter.format(calendar.getTime());

	}

	public static final Calendar getCalendarFromStringDateNoHHmmss(String date) {
		Calendar c = Calendar.getInstance();
		if (date == null) {
			return c;
		}
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyy-MM-dd");

		try {
			c.setTime(formatter.parse(date));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);

		return c;

	}
	
	

	public static final Calendar getCalendarFromString_yyyyMMdd(String date) {
		Calendar c = Calendar.getInstance();
		if (date == null) {
			return c;
		}
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");

		try {
			c.setTime(formatter.parse(date));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);

		return c;

	}

	public static final Calendar getCalendarFromStringImput(String stringImput, boolean incluyeHora) {

		if (stringImput == null) {
			return null;
		}

		Calendar c = Calendar.getInstance();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd" + (incluyeHora ? " hh:mm" : ""));
		
		if(incluyeHora && stringImput.length() > 16) {
			stringImput = stringImput.substring(0, 16);
		} else if(!incluyeHora && stringImput.length() > 10) {
			stringImput = stringImput.substring(0, 10);
		}
		
		stringImput = stringImput.replace("T", " ");

		try {
			c.setTime(formatter.parse(stringImput));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (!incluyeHora) {
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.clear(Calendar.MINUTE);
			c.clear(Calendar.SECOND);
			c.clear(Calendar.MILLISECOND);
		}

		return c;

	}

	public static final String getStringCalendarInput(Calendar calendarInput, boolean incluirHora) {

		try {
			if (calendarInput == null) {
				return "";
			}

			DateFormat fechaformatter = new SimpleDateFormat("yyyy-MM-dd");
			String fecha = fechaformatter.format(calendarInput.getTime());
			if (incluirHora) {
				DateFormat horaformatter = new SimpleDateFormat("HH:mm");
				fecha = fecha + "T" + horaformatter.format(calendarInput.getTime());
			}
			return fecha;
		} catch (Exception e) {
			return null;
		}

	}
	
	public static final String getStringCalendarForQuery(Calendar calendarInput, boolean incluirHora) {

		try {
			if (calendarInput == null) {
				return "";
			}

			DateFormat fechaformatter = new SimpleDateFormat("yyyy-MM-dd");
			String fecha = fechaformatter.format(calendarInput.getTime());
			if (incluirHora) {
				DateFormat horaformatter = new SimpleDateFormat("HH:mm:ss");
				fecha = fecha + " " + horaformatter.format(calendarInput.getTime());
			}
			return fecha;
		} catch (Exception e) {
			return null;
		}

	}

	public static final String getStringCalendarInterfaz(Calendar fecha, boolean incluirHora) {

		try {
			if(fecha == null) {
				return null;
			}
			
			DateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy" + (incluirHora ? " HH:mm:ss" : ""));
			
			String result = formatter.format(fecha.getTime());
			return result;
					
		} catch (Exception e) {
			return null;
		}

	}
	
	public static final String getStringCalendarInterfazSinSegundos(Calendar fecha, boolean incluirHora) {

		try {
			if(fecha == null) {
				return null;
			}
			
			DateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy" + (incluirHora ? " HH:mm" : ""));
			return formatter.format(fecha.getTime());
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * Devuelve la fecha con el formato 'yyyy.MM.dd'
	 * 
	 * @param calendar
	 * @return
	 */
	public static final String getStringFromCalendarForArchive(Calendar calendar) {

		try {
			DateFormat formatter;
			formatter = new SimpleDateFormat("yyyy.MM.dd");
			return formatter.format(calendar.getTime());
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Devuelve la fecha con el formato 'dd/mm/yyy'
	 * 
	 * @param calendar
	 * @return
	 */
	public static final String getStringFromCalendarForSimplePrint(Calendar calendar) {

		try {
			if(calendar == null) {
				return "NO IDENTIFICADO";
			}
			
			DateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy");
			return formatter.format(calendar.getTime());
		} catch (Exception e) {
			return null;
		}
	}

	public static final String getStringDate(Date date) {
		try {
			DateFormat formatter;
			formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
			return formatter.format(date);
		} catch (Exception e) {
			return null;
		}
	}

	public static final String getStringDateSinHora(Date date, int tipoFormat) {
		try {
			DateFormat formatter;
			formatter = new SimpleDateFormat(tipoFormat == 1 ? "yyyy-MM-dd" : "dd/MM/yyyy");
			return formatter.format(date);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static final String getStringDateNameFile(Date date) {
		try {
			DateFormat formatter;
			formatter = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
			return formatter.format(date);
		} catch (Exception e) {
			return null;
		}
	}

	public static final Long getDiferenciaDias(Calendar fecha1, String fecha2) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();

		try {
			c.setTime(sdf.parse(fecha2));

			DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);

			long fechaInicialMs = fecha1.getTime().getTime();
			long fechaFinalMs = c.getTime().getTime();
			long diferencia = fechaFinalMs - fechaInicialMs;
			double dias = Math.floor(diferencia / (1000 * 60 * 60 * 24));
			return ((long) dias);

		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * 
	 * @param date
	 * @param tipo       [1] fecha Desde [2] fecha Hasta
	 * @param formatType [1] 'dd/MM/yyyy' [2] 'yyyy-MM-dd'
	 * @return
	 * @throws ParseException
	 */
	public static final Calendar getCalendarFromString(String date, int tipo, int formatType) throws ParseException {

		Calendar c = Calendar.getInstance();

		try {

			if (date == null || date.trim().length() == 0) {
				return Calendar.getInstance();
			}

			String formato = (formatType == 1 ? "dd/MM/yyyy" : "yyyy-MM-dd");

			SimpleDateFormat sdf = new SimpleDateFormat(formato);

			if (tipo == 1) {
				// DESDE
				if (date != null && date.length() == 10) {
					c.setTime(sdf.parse(date));
				} else {
					c.set(Calendar.DAY_OF_MONTH, 1);
				}
				c.set(Calendar.HOUR_OF_DAY, 0);
				c.clear(Calendar.MINUTE);
				c.clear(Calendar.SECOND);
				c.clear(Calendar.MILLISECOND);
			}  else {
				// HASTA
				if (date != null && date.length() == 10) {
					c.setTime(sdf.parse(date));
				}

				c.set(Calendar.HOUR_OF_DAY, 0);
				c.clear(Calendar.MINUTE);
				c.clear(Calendar.SECOND);
				c.clear(Calendar.MILLISECOND);
				// Le sumo un dia y le resto un milisegundo
				c.add(Calendar.DATE, 1);
				c.add(Calendar.MILLISECOND, -1);
			}

		} catch (Exception e) {
			if (tipo == 1) {
				// DESDE
				c.set(Calendar.DAY_OF_MONTH, 1);
				c.set(Calendar.HOUR_OF_DAY, 0);
				c.clear(Calendar.MINUTE);
				c.clear(Calendar.SECOND);
				c.clear(Calendar.MILLISECOND);
			} else {
				// HASTA
				c.set(Calendar.HOUR_OF_DAY, 0);
				c.clear(Calendar.MINUTE);
				c.clear(Calendar.SECOND);
				c.clear(Calendar.MILLISECOND);
				// Le sumo un dia y le resto un milisegundo
				c.add(Calendar.DATE, 1);
				c.add(Calendar.MILLISECOND, -1);
			}
		}

		return c;

	}

	public static String getStringDDMMAA_FromTimestamp(long timestamp) {

		SimpleDateFormat sf = new SimpleDateFormat("ddMMyyyy");
		Date date = new Date(timestamp);
		// System.out.println(sf.format(date));

		return sf.format(date);
	}

	public static String changeFormatInterfaz(String fecha) {
		Calendar fechaAux = getCalendarFromStringDateNoHHmmss(fecha);
		fecha = getStringCalendarInterfaz(fechaAux, false);
		return fecha;
	}

}
