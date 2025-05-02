let path=require("path");
let fastify=require("fastify")({logger: false});
let fastifyStatic=require("@fastify/static");
let port=6006;
fastify.register(fastifyStatic,{
    root: path.join(__dirname),
    prefix: "/",
    index: "index.html",
    setHeaders: (res)=>{
        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    }
});
fastify.setNotFoundHandler((request, reply)=>{
    reply.code(404).type("text/html; charset=utf-8").send("<h1>404 Not Found</h1>");
});
fastify.setErrorHandler((error, request, reply)=>{
    reply.code(500).type("text/plain; charset=utf-8").send(`Server error: ${error.message}`);
});
let start=async ()=>{
    try{
        await fastify.listen({ port, host: "::" });
        console.log(`Server running at http://localhost:${port}`);
    }
    catch (err){
        fastify.log.error(err);
        process.exit(1);
    }
};
start();