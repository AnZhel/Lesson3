package http;

/**
 * Created by Andzhel on 21.01.2016.
 */
public class CacheChecker {
    public byte[] data;
    private long timeStamp;
    private long lifeTime;

    public CacheChecker(byte[] data, long lifeTime){
        this.data = data;
        this.lifeTime = lifeTime;
        this.timeStamp = System.currentTimeMillis();
    }

    public boolean isTimeToKill(){
        System.out.println("Время жизни кэша "+(System.currentTimeMillis() - timeStamp));
        return (System.currentTimeMillis() - timeStamp) >= lifeTime;
    }
}
