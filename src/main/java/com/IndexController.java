package com;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.concurrent.TimeUnit;

/*
*  三种方式实现分布式锁：redis性能最佳，缺点redis主从切换偶尔锁失效，Zookeeper集群架构天生为分布式锁准备的，绝对不出任何问题
* 《利用redis单线程SETNX方法》
* 《基于redission框架实现分布式锁》
* 《基于Zookeeper实现分布式锁》
* */
@Controller
public class IndexController {

    /*
    *   @Autowired Field injection is not recommended（Spring IOC不再推荐使用属性注入）
    *   下面将展示了spring框架可以使用的不同类型的依赖注入，以及每种依赖注入的适用情况。
    *   基于构造函数的依赖注入
    *   基于setter的依赖注入
    *   基于字段的依赖注入
    * */

//      基于Setter的依赖注入
        private Redisson redisson;
        @Autowired
        public void setRedisson(Redisson redisson){
            this.redisson = redisson;
        }

        private StringRedisTemplate stringRedisTemplate;
        @Autowired
        public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate){
            this.stringRedisTemplate = stringRedisTemplate;
        }

        /*
        *   《单机锁:synchronized》
        *  */
//        @RequestMapping("/deductStock")
//        @ResponseBody
//        public synchronized String deductStock(){
//            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
//            if(stock > 0){
//                int realStock = stock - 1;
//                stringRedisTemplate.opsForValue().set("stock", realStock + "");
//                System.out.println("扣减成功，剩余库存：" + realStock);
//            }else{
//                System.out.println("扣减失败，库存不足");
//            }
//            return "end";
//        }

        /*
            《利用redis单线程SETNX方法》
        *   问题1：避免加锁后删除锁时发生错误，造成死锁。
        *   解决方案：设置过期时间
        *   问题2：在setnx和expice之间程序发生错误，当前死锁无法释放
        *   解决方案：设置原子操作，加锁与设置时间同时进行
        *   问题3：高并发场景锁永久失效，避免超时程序未执行完，锁被其他线程释放了
        *   解决方案:给当前商品设置唯一标识，释放锁前判断是否是当前锁
        *   问题4：如何处理锁失效程序未执行完
        *   解决方案:开启子线程，利用定时器不断的重复，判断这把锁（商品id）与（唯一标识）是否一致，则延长超时时间的三分之一，
        * */
//        @RequestMapping("/deductStock")
//        @ResponseBody
//        public synchronized String deductStock(){
//            //商品
//            String product = "product_001";
//            //唯一标识
//            String clientid = UUID.randomUUID().toString();
//            try {
////          Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(product, "id");
////          stringRedisTemplate.expire("product",10, TimeUnit.SECONDS);
//            //设置原子操作，加锁与设置时间同时进行
//            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(product,clientid,10, TimeUnit.SECONDS);
//            if(!result){
//                return "你好，系统繁忙请稍后再试！";
//            }
//                int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
//                if(stock > 0){
//                    int realStock = stock - 1;
//                    stringRedisTemplate.opsForValue().set("stock", realStock + "");
//                    System.out.println("扣减成功，剩余库存：" + realStock);
//                }else{
//                    System.out.println("扣减失败，库存不足");
//                }
//            }finally {
//                //判断当前线程的唯一标识和商品id是否一致
//                if(clientid.equals(stringRedisTemplate.opsForValue().get(product))){
//                    //删除锁
//                    stringRedisTemplate.delete(product);
//                }
//            }
//            return "end";
//        }

        /*
        *   基于Redisson实现分布式锁
        * */
        @RequestMapping("/deductStock")
        @ResponseBody
        public synchronized String deductStock(){
            //商品
            String product = "product_001";
            //获取锁
            RLock redissonlock = redisson.getLock(product);

            try {
                //设置超时时间
                redissonlock.lock(30, TimeUnit.SECONDS);

                int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
                if(stock > 0){
                    int realStock = stock - 1;
                    stringRedisTemplate.opsForValue().set("stock", realStock + "");
                    System.out.println("扣减成功，剩余库存：" + realStock);
                }else{
                    System.out.println("扣减失败，库存不足");
                }
            }finally {
                //释放锁
                redissonlock.unlock();
            }

            return "end";
        }
}
