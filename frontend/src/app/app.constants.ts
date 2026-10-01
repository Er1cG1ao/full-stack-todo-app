/**
 * 全局常量。
 *
 * class2 里每个 service 方法都把 'http://localhost:8080' 写死在字符串里，
 * 一旦后端换端口/换域名就要改十几处。集中成一个常量是最低成本的改进。
 */
export const API_URL = 'http://localhost:8080';
