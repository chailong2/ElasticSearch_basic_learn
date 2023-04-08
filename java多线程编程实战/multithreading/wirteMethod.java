import java.util.Random;

public class C {
    private String lock;
    public C(String lock){
        super();
        this.lock=lock;
    }
    public void GetValue(){
        try{
            synchronized (lock){
                while(!ValueObject.value.equals("")){   //value不为空说明消费者还没有消费，所以生产者需要等待
                    lock.wait();
                }
                lock.notify();
                String value= String.valueOf(System.currentTimeMillis())+"_"+String.valueOf(System.nanoTime());
                System.out.println("生产的产品："+value);
                ValueObject.value=value;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
