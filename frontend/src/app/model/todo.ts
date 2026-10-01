/**
 * 后端 Todo 的前端映射类型。
 *
 * 对应 Spring Boot 侧的：
 *   public class Todo {
 *     private long id;
 *     private String username;
 *     private String description;
 *     private Date targetDate;
 *     private boolean done;
 *   }
 *
 * 注意 targetDate 是 string 而不是 Date：
 * JSON 里没有日期类型，后端序列化出来的是 "2026-08-01T00:00:00.000+00:00" 这样的字符串，
 * HttpClient 只做 JSON.parse，不会帮你还原成 Date 对象。
 * 想显示成日期，交给模板里的 `| date` 管道即可。
 */
export interface Todo {
  id: number;
  username?: string;
  description: string;
  targetDate: string;
  done: boolean;
}
