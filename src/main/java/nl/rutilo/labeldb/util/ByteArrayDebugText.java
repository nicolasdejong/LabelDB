package nl.rutilo.labeldb.util;

public abstract class ByteArrayDebugText {
    public static String toDebugString(byte[] data) { return toDebugString(data, 16); }
    public static String toDebugString(byte[] bytes, int lineByteCount) {
        final StringBuilder sb = new StringBuilder();
        int offset = 0;
        while(offset < bytes.length) {
            final String label = Integer.toHexString(offset);
            sb.append( "0000".substring(0, 5-label.length()) + label );
            sb.append( ": ");
            for(int i=0; i<lineByteCount; i++) {
                if(offset + i < bytes.length) {
                    int value = bytes[offset + i];
                    if (value < 0) value += 256;
                    final String hex = Integer.toHexString(value);
                    if (hex.length() < 2) sb.append("0");
                    sb.append(hex);
                } else {
                    sb.append("  ");
                }
                sb.append(" ");
            }
            sb.append(" ");
            for(int i=0; i<lineByteCount && offset + i < bytes.length; i++) {
                int value = bytes[offset+i]; if(value < 0) value += 256;
                sb.append( value >= 32 && value <= 126 ? (char)value : '.' );
            }
            sb.append("\n");
            offset += lineByteCount;
        }
        return sb.toString();
    }
    public static void print(byte[] data) { System.out.println(toDebugString(data)); }
}
